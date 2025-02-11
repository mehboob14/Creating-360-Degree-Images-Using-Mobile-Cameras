import cv2 as cv
import numpy as np
import sys

def load_image(img_path):
    """Loads an image from a given path."""
    img = cv.imread(img_path)
    if img is None:
        raise ValueError(f"Error loading image: {img_path}")
    return img

def extract_features(image):
    """Extracts keypoints and descriptors using SIFT."""
    detector = cv.SIFT_create()
    keypoints, descriptors = detector.detectAndCompute(image, None)
    return keypoints, descriptors

def match_features(desc1, desc2, ratio=0.75):
    """Matches features using KNN and applies Lowe’s ratio test."""
    bf = cv.BFMatcher(cv.NORM_L2, crossCheck=False)
    raw_matches = bf.knnMatch(desc1, desc2, k=2)
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
    
    corners_img1 = np.float32([[0, 0], [0, h1], [w1, h1], [w1, 0]]).reshape(-1, 1, 2)
    corners_transformed = cv.perspectiveTransform(corners_img1, H)
    
    corners_img2 = np.float32([[0, 0], [0, h2], [w2, h2], [w2, 0]]).reshape(-1, 1, 2)
    all_corners = np.concatenate((corners_transformed, corners_img2), axis=0)
    
    x_min, y_min = np.int32(all_corners.min(axis=0).flatten())
    x_max, y_max = np.int32(all_corners.max(axis=0).flatten())
    
    translation_matrix = np.array([[1, 0, -x_min], [0, 1, -y_min], [0, 0, 1]])
    
    result_size = (x_max - x_min, y_max - y_min)
    result = cv.warpPerspective(img1, translation_matrix @ H, result_size)
    
    result[-y_min:h2 - y_min, -x_min:w2 - x_min] = img2
    return result

def stitch_multiple_images(image_paths):
    """Stitches multiple images together one by one."""
    if len(image_paths) < 2:
        raise ValueError("At least two images are required for stitching.")
    
    stitched_image = load_image(image_paths[0])
    
    for i in range(1, len(image_paths)):
        next_image = load_image(image_paths[i])
        
        img1_gray = cv.cvtColor(stitched_image, cv.COLOR_BGR2GRAY)
        img2_gray = cv.cvtColor(next_image, cv.COLOR_BGR2GRAY)
        
        kp1, desc1 = extract_features(img1_gray)
        kp2, desc2 = extract_features(img2_gray)
        
        matches = match_features(desc1, desc2)
        if len(matches) < 4:
            print(f"Not enough matches found between images {i} and {i+1}.")
            continue
        
        H = find_homography(kp1, kp2, matches)
        stitched_image = warp_and_stitch(stitched_image, next_image, H)
    
    return stitched_image

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python Multiple_images.py img1.jpg img2.jpg [img3.jpg ... imgN.jpg]")
        sys.exit(1)
    
    image_paths = sys.argv[1:]
    final_stitched_image = stitch_multiple_images(image_paths)
    
    cv.imshow("Stitched Image", final_stitched_image)
    cv.imwrite("stitched_output.jpg", final_stitched_image)
    cv.waitKey(0)
    cv.destroyAllWindows()

