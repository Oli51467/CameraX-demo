from Support import *
import numpy as np
import cv2

def main(byte_array):
    img = np.asarray(byte_array, dtype="uint8")
    img = cv2.imdecode(img, -1)  # cv2.imdecode()函数从指定的内存缓存中读取数据, 并把数据转换(解码)成图像格式, 主要用于从网络传输数据中恢复出图像。
    shape = img.shape
    return shape[0]
