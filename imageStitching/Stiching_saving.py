import cv2 as cv
import numpy as np

# Load images
train_img = cv.imread('train.jpg')
train_img_gray = cv.cvtColor(train_img, cv.COLOR_BGR2GRAY)

query_img = cv.imread('query.jpg')
query_img_gray = cv.cvtColor(query_img, cv.COLOR_BGR2GRAY)


feature_extraction_algo = 'sift'

def select_descriptor_method(image, method=None):
    if method == 'sift':
        descriptor = cv.SIFT_create()
    elif method == 'surf':
        descriptor = cv.SURF_create()
    elif method == 'orb':
        descriptor = cv.ORB_create()
    elif method == 'brisk':
        descriptor = cv.BRISK_create()
    else:
        raise ValueError("Unsupported feature extraction method.")
    
    keypoints, features = descriptor.detectAndCompute(image, None)
    return keypoints, features

def create_matching_object(method, crossCheck=True):
    if method in ['sift', 'surf']:
        bf = cv.BFMatcher(cv.NORM_L2, crossCheck=crossCheck)
    elif method in ['orb', 'brisk']:
        bf = cv.BFMatcher(cv.NORM_HAMMING, crossCheck=crossCheck)
    else:
        raise ValueError("Unsupported matching method.")
    return bf

def key_points_matching(features_train_img, features_query_img, method):
    bf = create_matching_object(method=method, crossCheck=True)
    matches = bf.match(features_train_img, features_query_img)
    return sorted(matches, key=lambda x: x.distance)

keypoints_train_img, features_train_img = select_descriptor_method(train_img_gray, feature_extraction_algo)
keypoints_query_img, features_query_img = select_descriptor_method(query_img_gray, feature_extraction_algo)


matches = key_points_matching(features_query_img, features_train_img, feature_extraction_algo)


if len(matches) >= 4:
    valid_matches = [
        m for m in matches
        if m.queryIdx < len(keypoints_query_img) and m.trainIdx < len(keypoints_train_img)
    ]

    if len(valid_matches) >= 4:
        src_pts = np.float32([keypoints_query_img[m.queryIdx].pt for m in valid_matches]).reshape(-1, 1, 2)
        dst_pts = np.float32([keypoints_train_img[m.trainIdx].pt for m in valid_matches]).reshape(-1, 1, 2)

        
        H, mask = cv.findHomography(src_pts, dst_pts, cv.RANSAC, 5.0)

        height_train, width_train = train_img.shape[:2]
        height_query, width_query = query_img.shape[:2]

        corners_query = np.float32([
            [0, 0],
            [width_query - 1, 0],
            [width_query - 1, height_query - 1],
            [0, height_query - 1]
        ]).reshape(-1, 1, 2)
        corners_query_transformed = cv.perspectiveTransform(corners_query, H)

        corners_train = np.float32([
            [0, 0],
            [width_train - 1, 0],
            [width_train - 1, height_train - 1],
            [0, height_train - 1]
        ]).reshape(-1, 1, 2)

        all_corners = np.concatenate((corners_train, corners_query_transformed), axis=0)
        [x_min, y_min] = np.int32(all_corners.min(axis=0).ravel() - 0.5)
        [x_max, y_max] = np.int32(all_corners.max(axis=0).ravel() + 0.5)

        translation_dist = [-x_min, -y_min]
        translation_matrix = np.array([
            [1, 0, translation_dist[0]],
            [0, 1, translation_dist[1]],
            [0, 0, 1]
        ])

        result_size = (x_max - x_min, y_max - y_min)
        result_img = cv.warpPerspective(query_img, translation_matrix @ H, result_size)

        y1, y2 = translation_dist[1], translation_dist[1] + height_train
        x1, x2 = translation_dist[0], translation_dist[0] + width_train

        h1, w1 = result_img[y1:y2, x1:x2].shape[:2]
        h2, w2 = train_img.shape[:2]
        h, w = min(h1, h2), min(w1, w2)

        result_img_cropped = result_img[y1:y1+h, x1:x1+w]
        train_img_cropped = train_img[:h, :w]

        result_img[y1:y1+h, x1:x1+w] = np.maximum(result_img_cropped, train_img_cropped)

        cv.imwrite('stitched_result.jpg', result_img)
        cv.imshow('Stitched Image', result_img)
        cv.waitKey(0)
        cv.destroyAllWindows()
    else:
        print("Not enough valid matches to compute homography.")
else:
    print("Not enough matches found to compute homography.")
