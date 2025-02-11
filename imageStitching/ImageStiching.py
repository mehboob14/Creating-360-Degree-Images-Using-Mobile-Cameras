import cv2 as cv
import numpy as np 
import matplotlib.pyplot as plt

train_img = cv.imread('train.jpg')
train_img_gray = cv.cvtColor(train_img,cv.COLOR_BGR2GRAY)

query_img = cv.imread('query.jpg')
query_img_gray = cv.cvtColor(query_img,cv.COLOR_BGR2GRAY)

feature_extraction_algo = 'sift'
feature_to_match = 'bf'

# cv.imshow('train_iamge',train_img)
# cv.imshow('query_image',query_img)
# cv.waitKey(0)
# cv.destroyAllWindows()

# cv.imshow('train_gray_image',train_img_gray)
# cv.waitKey(0)
# cv.destroyAllWindows()


def select_descriptor_method(image,method=None):

    if method=='sift':
        descriptor = cv.SIFT_create()
    if method =='surf':
        descriptor = cv.SURF_create()
    if method =='orb':
        descriptor = cv.ORB_create()
    if method == 'brisk':
        descriptor = cv.BRISK_create()

    (keypoints,features) = descriptor.detectAndCompute(image,None)
    return (keypoints,features)
    #----------
def create_matching_object(method,crossCheck):
    if method == 'sift' or method == 'surf':
        bf = cv.BFMatcher(cv.NORM_L2,crossCheck = crossCheck)
    if method == 'orb' or method=="brisk":
        bf = cv.BFMatcher(cv.NORM_HAMMING,crossCheck=crossCheck)

    return bf
    #--------
def key_points_matching(features_train_img,features_query_img,method):
    bf = create_matching_object(method=method,crossCheck=True)
    matches = bf.match(features_train_img,features_query_img)

    best_matches = sorted(matches, key=lambda x: x.distance)

    return best_matches

keypoints_train_img,features_train_img = select_descriptor_method(train_img_gray,feature_extraction_algo)
keypoints_query_img,features_query_img = select_descriptor_method(query_img_gray,feature_extraction_algo)

train_img_with_keypoints = cv.drawKeypoints(train_img,keypoints_train_img,None,color=(0,255,0),flags=None)
# cv.imshow("train image with keypoints",train_img_with_keypoints)
# cv.waitKey(0)
# cv.destroyAllWindows()


def key_points_matching_KNN(features_train_img, features_query_img, ratio, method):
   
    bf = create_matching_object(method, crossCheck=False)

    rawMatches = bf.knnMatch(features_train_img, features_query_img, k=2)
    print("Raw matches (knn):", len(rawMatches))
    matches = []

    for m,n in rawMatches:

        if m.distance < n.distance * ratio:
            matches.append(m)
    return matches


matches = key_points_matching(features_train_img,features_query_img,feature_extraction_algo)
result_direct = cv.drawMatches(
                               query_img, keypoints_query_img,
                               train_img, keypoints_train_img,
                               matches[:100], None, flags=cv.DrawMatchesFlags_NOT_DRAW_SINGLE_POINTS)    

knnMatches = key_points_matching_KNN(features_train_img,features_query_img,0.75,feature_extraction_algo)
result_direct_knn = cv.drawMatches(train_img, keypoints_train_img,
                               query_img, keypoints_query_img,
                               matches[:100], None, flags=cv.DrawMatchesFlags_NOT_DRAW_SINGLE_POINTS) 



# cv.imshow("Direct Matches", result_direct)
# # cv.imshow("knn_matching",result_direct_knn)
# cv.waitKey(0)
# cv.destroyAllWindows()
