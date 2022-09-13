from Support import *
import numpy as np
import cv2

def main(byte_array):
    img = np.asarray(byte_array, dtype="uint8")
    img = cv2.imdecode(img, -1)
    shape = img.shape
    return shape[2]
