# YAMNet Model

## Required Model File
Place the `yamnet.tflite` model file in this directory.

## Download Instructions
1. Download YAMNet model from TensorFlow Hub:
   https://tfhub.dev/google/lite-model/yamnet/tflite/1
   
2. Or download from MediaPipe:
   https://storage.googleapis.com/mediapipe-assets/YAMNet

## Model Specifications
- Input: Float32 tensor [1, 15600] (16kHz audio, 0.96 seconds)
- Output: Float32 tensor [1, 521] (AudioSet class probabilities)
- Snoring class: Index 489 in AudioSet taxonomy
- Confidence threshold: 0.35 (configurable in SnoreDetector)

## Integration
The SnoreDetector class loads this model from assets and performs inference
on a background thread, emitting SnoreEvent when snoring is detected.
