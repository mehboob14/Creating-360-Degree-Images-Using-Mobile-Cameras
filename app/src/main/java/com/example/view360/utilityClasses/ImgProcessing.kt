package com.example.view360.utilityClasses

import android.util.Log
import org.opencv.calib3d.Calib3d
import org.opencv.core.Core
import org.opencv.core.Core.NORM_HAMMING
import org.opencv.core.Core.NORM_L2
import org.opencv.core.CvType
import org.opencv.core.KeyPoint
import org.opencv.core.Mat
import org.opencv.features2d.BRISK
import org.opencv.features2d.Feature2D
import org.opencv.features2d.ORB
import org.opencv.features2d.SIFT
import org.opencv.core.DMatch
import org.opencv.core.MatOfDMatch
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Range
import org.opencv.core.Size
import org.opencv.features2d.BFMatcher
import org.opencv.imgproc.Imgproc
import org.opencv.imgcodecs.Imgcodecs




class ImgProcessing(img1Path: String, img2Path: String) {
    private val img1: Mat = Imgcodecs.imread(img1Path, Imgcodecs.IMREAD_COLOR)
    private val img2: Mat = Imgcodecs.imread(img2Path, Imgcodecs.IMREAD_COLOR)
    private lateinit var keypointsImg1: List<KeyPoint>
    private lateinit var keypointsImg2: List<KeyPoint>
    private lateinit var matches: List<DMatch>

    init {


        require(!img1.empty() && !img2.empty()) { "Failed to load one or both images." }

        resizeImg(img1,800)
        resizeImg(img2,800)

        val img1Gray = Mat()
        Imgproc.cvtColor(img1, img1Gray, Imgproc.COLOR_BGR2GRAY)

        val img2Gray = Mat()
        Imgproc.cvtColor(img2, img2Gray, Imgproc.COLOR_BGR2GRAY)


        val featureExtractionAlgo = "sift"

        val (keypointsImg1, featuresImg1) = selectDescriptorMethod(img1Gray, featureExtractionAlgo)
        val (keypointsImg2, featuresImg2) = selectDescriptorMethod(img2Gray, featureExtractionAlgo)

        this.keypointsImg1 = keypointsImg1.toList()
        this.keypointsImg2 = keypointsImg2.toList()

        matches = keyPointsMatching(featuresImg2, featuresImg1, featureExtractionAlgo)

    }


    fun loadImage(imagePath: String): Mat {
        val image = Imgcodecs.imread(imagePath, Imgcodecs.IMREAD_COLOR)
        require(!image.empty()) { "Failed to load image from path: $imagePath" }
        return image
    }



    fun selectDescriptorMethod(image: Mat, method: String?): Pair<MatOfKeyPoint, Mat> {
        val descriptor: Feature2D = when (method) {
            "sift" -> SIFT.create()
            "orb" -> ORB.create()
            "brisk" -> BRISK.create()
            else -> throw IllegalArgumentException("Invalid method")
        }

        val keypoints = MatOfKeyPoint()
        val features = Mat()
        descriptor.detectAndCompute(image, Mat(), keypoints, features)

        return Pair(keypoints, features)
    }



    fun createMatchingObject(method: String, crossCheck: Boolean): BFMatcher {
        return when (method) {
            "sift" -> BFMatcher(NORM_L2, crossCheck)
            "orb", "brisk" -> BFMatcher(NORM_HAMMING, crossCheck)
            else -> throw IllegalArgumentException("Invalid method")
        }
    }

    fun keyPointsMatching(img1Features: Mat, img2Features: Mat, method: String): List<DMatch> {
        val bf = createMatchingObject(method, crossCheck = true)
        val matches = MatOfDMatch()
        bf.match(img1Features, img2Features, matches)
        val bestMatches = matches.toList().sortedBy { it.distance }

        return bestMatches
    }

    fun resizeImg(img: Mat, targetWidth: Int) {
        val resizedImg = Mat()
//        val targetWidth = 800.0
        val aspectRatio = img.height() / img.width().toDouble()
        val targetHeight = targetWidth * aspectRatio

        Imgproc.resize(img, img, Size(targetWidth.toDouble(), targetHeight))
    }


    fun stitchImgs(): Mat? {
        if (matches.size >= 4) {
            val validMatches = matches.filter { it.queryIdx < keypointsImg2.size && it.trainIdx < keypointsImg1.size }

            if (validMatches.size >= 4) {
                val srcPts = MatOfPoint2f(*validMatches.map { keypointsImg2[it.queryIdx].pt }.toTypedArray())
                val dstPts = MatOfPoint2f(*validMatches.map { keypointsImg1[it.trainIdx].pt }.toTypedArray())

                val mask = Mat()
                val homography = Calib3d.findHomography(srcPts, dstPts, Calib3d.RANSAC, 5.0, mask)
                if (homography.empty()) {
                    throw IllegalArgumentException("Failed to compute homography")
                    return null
                }

                val img1Size = img1.size()
                val img2Size = img2.size()

                // Define corners for img2
                val cornersImg2 = MatOfPoint2f(
                    Point(0.0, 0.0), Point(img2Size.width - 1.0, 0.0),
                    Point(img2Size.width - 1.0, img2Size.height - 1.0), Point(0.0, img2Size.height - 1.0)
                )
                val cornersImg2Transformed = MatOfPoint2f()
                Core.perspectiveTransform(cornersImg2, cornersImg2Transformed, homography)

                // Define corners for img1
                val cornersImg1 = MatOfPoint2f(
                    Point(0.0, 0.0), Point(img1Size.width - 1.0, 0.0),
                    Point(img1Size.width - 1.0, img1Size.height - 1.0), Point(0.0, img1Size.height - 1.0)
                )

                // Combine corners into a single Mat
                val allCorners = Mat()
                Core.vconcat(listOf(cornersImg1, cornersImg2Transformed), allCorners)

                // Extract x and y coordinates separately
                val xCoords = Mat(allCorners.rows(), 1, CvType.CV_32F)
                val yCoords = Mat(allCorners.rows(), 1, CvType.CV_32F)
                Core.extractChannel(allCorners, xCoords, 0) // Extract x coordinates (channel 0)
                Core.extractChannel(allCorners, yCoords, 1) // Extract y coordinates (channel 1)

                // Compute min and max for x and y coordinates
                val xMinMax = Core.minMaxLoc(xCoords)
                val yMinMax = Core.minMaxLoc(yCoords)

                val xMin = xMinMax.minVal
                val yMin = yMinMax.minVal
                val xMax = xMinMax.maxVal
                val yMax = yMinMax.maxVal

                // Calculate translation distance
                val translationDist = Point(-xMin, -yMin)

                // Create translation matrix
                val translationMatrix = Mat.eye(3, 3, CvType.CV_64F)
                translationMatrix.put(0, 2, translationDist.x)
                translationMatrix.put(1, 2, translationDist.y)

                // Compute result size
                val resultSize = Size(xMax - xMin, yMax - yMin)

                // Warp img2 using the combined transformation matrix
                val resultImg = Mat()
                val transformedMatrix = Mat()
                Core.gemm(translationMatrix, homography, 1.0, Mat(), 0.0, transformedMatrix)
                Imgproc.warpPerspective(img2, resultImg, transformedMatrix, resultSize)

                // Define cropping regions
                val y1 = translationDist.y.toInt()
                val y2 = y1 + img1Size.height.toInt()
                val x1 = translationDist.x.toInt()
                val x2 = x1 + img1Size.width.toInt()

                // Ensure cropping regions are within bounds
                if (y1 < 0 || y2 > resultImg.rows() || x1 < 0 || x2 > resultImg.cols()) {
                    throw IllegalArgumentException("Invalid cropping region")
                    return null
                }

                // Crop and blend the images
                val resultImgCropped = resultImg.submat(Range(y1, y2), Range(x1, x2))
                val img1Cropped = img1.submat(Range(0, img1Size.height.toInt()), Range(0, img1Size.width.toInt()))

                // Blend the images using maximum intensity
                Core.max(resultImgCropped, img1Cropped, resultImg.submat(Range(y1, y2), Range(x1, x2)))

                return resultImg
            } else {

                throw IllegalArgumentException("could not find valid enough matches")
            }
        } else {

            throw IllegalArgumentException("could not find enough matches")
        }
        return null
    }

}