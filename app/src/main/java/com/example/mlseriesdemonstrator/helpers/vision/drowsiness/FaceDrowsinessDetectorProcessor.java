// Define o pacote ao qual esta classe pertence
package com.example.mlseriesdemonstrator.helpers.vision.drowsiness;

// Importações de bibliotecas e classes necessárias
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;

import com.example.mlseriesdemonstrator.helpers.vision.FaceGraphic;
import com.example.mlseriesdemonstrator.helpers.vision.GraphicOverlay;
import com.example.mlseriesdemonstrator.helpers.vision.VisionBaseProcessor;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/** Documentação da classe para demonstrar a detecção de sonolência facial. */
public class FaceDrowsinessDetectorProcessor extends VisionBaseProcessor<List<Face>> {

  private static final String MANUAL_TESTING_LOG = "FaceDetectorProcessor"; // Tag para logs de teste manual

  private final FaceDetector detector; // O detector de rostos
  private FaceDrowsiness faceDrowsiness; // Objeto para calcular a sonolência
  private final GraphicOverlay graphicOverlay; // Overlay gráfico para desenhar os resultados
  private final HashMap<Integer, FaceDrowsiness> drowsinessHashMap = new HashMap<>(); // Mapa para armazenar dados de sonolência

  // Construtor que configura o detector de rostos e o overlay gráfico
  public FaceDrowsinessDetectorProcessor(GraphicOverlay graphicOverlay) {
    this.graphicOverlay = graphicOverlay;
    this.faceDrowsiness = FaceDrowsiness.getInstance();
    // Opções para o detector de rostos, configurado para ser rápido e detectar todos os marcos e classificações
    FaceDetectorOptions faceDetectorOptions = new FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .enableTracking()
            .build();
    Log.v(MANUAL_TESTING_LOG, "Face detector options: " + faceDetectorOptions);
    detector = FaceDetection.getClient(faceDetectorOptions);
  }

  // Método para retornar a instância de FaceDrowsiness
  public FaceDrowsiness getFaceDrowsiness() {
    return faceDrowsiness;
  }

  // Método para detectar rostos na imagem fornecida
  @OptIn(markerClass = ExperimentalGetImage.class)
  public Task<List<Face>> detectInImage(ImageProxy imageProxy, Bitmap bitmap, int rotationDegrees) {
    InputImage inputImage = InputImage.fromMediaImage(imageProxy.getImage(), rotationDegrees);
    int rotation = rotationDegrees;

    // Lógica para ajustar as dimensões da imagem com base na rotação
    boolean reverseDimens = rotation == 90 || rotation == 270;
    int width;
    int height;
    if (reverseDimens) {
      width = imageProxy.getHeight();
      height = imageProxy.getWidth();
    } else {
      width = imageProxy.getWidth();
      height = imageProxy.getHeight();
    }

    // Processa a imagem com o detector e lida com os resultados
    return detector.process(inputImage)
            .addOnSuccessListener(faces -> {
              graphicOverlay.clear();
              for (Face face : faces) {
                FaceDrowsiness faceDrowsinessInstance = FaceDrowsiness.getInstance();
                boolean isDrowsy = faceDrowsinessInstance.isDrowsy(face);
                FaceGraphic faceGraphic = new FaceGraphic(graphicOverlay, face, isDrowsy, width, height);
                graphicOverlay.add(faceGraphic);
              }
            })
            .addOnFailureListener(new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception e) {
                // Logica para lidar com falhas no processamento
              }
            });
  }

  // Método para liberar recursos do detector
  public void stop() {
    detector.close();
  }

  // Método privado para logar informações extras para testes
  private static void logExtrasForTesting(Face face) {
    if (face != null) {
      // Logs detalhados das características faciais detectadas para fins de teste
      Log.v(MANUAL_TESTING_LOG, "face bounding box: " + face.getBoundingBox().flattenToString());
      Log.v(MANUAL_TESTING_LOG, "face Euler Angle X: " + face.getHeadEulerAngleX());
      Log.v(MANUAL_TESTING_LOG, "face Euler Angle Y: " + face.getHeadEulerAngleY());
      Log.v(MANUAL_TESTING_LOG, "face Euler Angle Z: " + face.getHeadEulerAngleZ());

      // Itera sobre os tipos de marcos faciais, registrando sua presença e posição
      int[] landMarkTypes = {
              FaceLandmark.MOUTH_BOTTOM,
              FaceLandmark.MOUTH_RIGHT,
              FaceLandmark.MOUTH_LEFT,
              FaceLandmark.RIGHT_EYE,
              FaceLandmark.LEFT_EYE,
              FaceLandmark.RIGHT_EAR,
              FaceLandmark.LEFT_EAR,
              FaceLandmark.RIGHT_CHEEK,
              FaceLandmark.LEFT_CHEEK,
              FaceLandmark.NOSE_BASE
      };
      String[] landMarkTypesStrings = {
              "MOUTH_BOTTOM",
              "MOUTH_RIGHT",
              "MOUTH_LEFT",
              "RIGHT_EYE",
              "LEFT_EYE",
              "RIGHT_EAR",
              "LEFT_EAR",
              "RIGHT_CHEEK",
              "LEFT_CHEEK",
              "NOSE_BASE"
      };
      for (int i = 0; i < landMarkTypes.length; i++) {
        FaceLandmark landmark = face.getLandmark(landMarkTypes[i]);
        if (landmark == null) {
          Log.v(MANUAL_TESTING_LOG, "No landmark of type: " + landMarkTypesStrings[i] + " has been detected");
        } else {
          PointF landmarkPosition = landmark.getPosition();
          String landmarkPositionStr = String.format(Locale.US, "x: %f , y: %f", landmarkPosition.x, landmarkPosition.y);
          Log.v(MANUAL_TESTING_LOG, "Position for face landmark: " + landMarkTypesStrings[i] + " is :" + landmarkPositionStr);
        }
      }

      // Logs adicionais para as probabilidades de abertura dos olhos e sorriso, e o ID de rastreamento
      Log.v(MANUAL_TESTING_LOG, "face left eye open probability: " + face.getLeftEyeOpenProbability());
      Log.v(MANUAL_TESTING_LOG, "face right eye open probability: " + face.getRightEyeOpenProbability());
      Log.v(MANUAL_TESTING_LOG, "face smiling probability: " + face.getSmilingProbability());
      Log.v(MANUAL_TESTING_LOG, "face tracking id: " + face.getTrackingId());
    }
  }
}
