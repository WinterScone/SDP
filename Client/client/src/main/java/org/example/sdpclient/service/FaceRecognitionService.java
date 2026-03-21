package org.example.sdpclient.service;

public interface FaceRecognitionService {

    byte[] prepareFaceData(byte[] imageBytes);

    boolean verify(byte[] enrolledFaceData, byte[] candidateImageBytes);
}
