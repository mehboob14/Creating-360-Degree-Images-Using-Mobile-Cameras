import cv2 as cv
import numpy as np
import sys

def load_images(img1_path, img2_path):
    """Loads two images from given paths."""
    img1 = cv.imread(img1_path)
    img2 = cv.imread(img2_path)

    if img1 is None or img2 is None:
        raise ValueError("Error loading images. Check the paths.")

    return img1, img2

def extract_features(image, method='sift'):
    """Extracts keypoints and descriptors using SIFT."""
    detector = cv.SIFT_create()
    keypoints, descriptors = detector.detectAndCompute(image, None)
    return keypoints, descriptors

def match_features(desc1, desc2, ratio=0.75):
    """Matches features using KNN and applies Lowe’s ratio test."""
    bf = cv.BFMatcher(cv.NORM_L2, crossCheck=False)
    raw_matches = bf.knnMatch(desc1, desc2, k=2)

    # Lowe’s ratio test
    matches = [m for m, n in raw_matches if m.distance < ratio * n.distance]
    return matches

def find_homography(kp1, kp2, matches):
    """Finds the homography matrix using RANSAC."""
    if len(matches) < 4:
        raise ValueError("Not enough matches found for homography estimation.")

    src_pts = np.float32([kp1[m.queryIdx].pt for m in matches]).reshape(-1, 1, 2)
    dst_pts = np.float32([kp2[m.trainIdx].pt for m in matches]).reshape(-1, 1, 2)

    H, _ = cv.findHomography(src_pts, dst_pts, cv.RANSAC)
    return H

def warp_and_stitch(img1, img2, H):
    """Warps img1 to align with img2 using the homography matrix and stitches them together."""
    h1, w1 = img1.shape[:2]
    h2, w2 = img2.shape[:2]

    # Get the canvas size after warping
    corners_img1 = np.float32([[0, 0], [0, h1], [w1, h1], [w1, 0]]).reshape(-1, 1, 2)
    corners_transformed = cv.perspectiveTransform(corners_img1, H)

    # Find the min and max points to define the size of the final stitched image
    corners_img2 = np.float32([[0, 0], [0, h2], [w2, h2], [w2, 0]]).reshape(-1, 1, 2)
    all_corners = np.concatenate((corners_transformed, corners_img2), axis=0)

    x_min, y_min = np.int32(all_corners.min(axis=0).flatten())
    x_max, y_max = np.int32(all_corners.max(axis=0).flatten())

    # Compute translation matrix to move the image into the valid region
    translation_matrix = np.array([[1, 0, -x_min], [0, 1, -y_min], [0, 0, 1]])

    # Warp the first image
    result_size = (x_max - x_min, y_max - y_min)
    result = cv.warpPerspective(img1, translation_matrix @ H, result_size)

    # Paste the second image
    result[-y_min:h2 - y_min, -x_min:w2 - x_min] = img2

    return result

def main(img1_path, img2_path):
    """Main function to run the stitching pipeline."""
    img1, img2 = load_images(img1_path, img2_path)

    # Convert to grayscale
    img1_gray = cv.cvtColor(img1, cv.COLOR_BGR2GRAY)
    img2_gray = cv.cvtColor(img2, cv.COLOR_BGR2GRAY)

    # Feature detection & matching
    kp1, desc1 = extract_features(img1_gray)
    kp2, desc2 = extract_features(img2_gray)
    matches = match_features(desc1, desc2)

    if len(matches) < 4:
        print("Not enough matches found.")
        return

    # Find homography & stitch
    H = find_homography(kp1, kp2, matches)
    stitched_image = warp_and_stitch(img1, img2, H)

    # Show and save result
    cv.imshow("Stitched Image", stitched_image)
    cv.imwrite("stitched_output.jpg", stitched_image)
    cv.waitKey(0)
    cv.destroyAllWindows()

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python Multiple_images.py train.jpg query.jpg")
        sys.exit(1)

    main(sys.argv[1], sys.argv[2])
