import cv2
import stitch
import utils
import timeit
import os

def create_panorama(input_dir, output_file, resize_flag=0):
    
    print("Processing....")
    start = timeit.default_timer()

    list_images = utils.loadImages(input_dir, resize_flag)

    panorama = stitch.multiStitching(list_images)

    cv2.imwrite(output_file, panorama)

    stop = timeit.default_timer()
    print("Complete!")
    print("Execution time: ", stop - start)

    return panorama

input_dir = "D:/Mehboob/Creating-360-Degree-Images-Using-Mobile-Cameras/Panorama/data/city"
output_file = "result.jpg"
resize_flag = 0

panorama = create_panorama(input_dir, output_file, resize_flag)
