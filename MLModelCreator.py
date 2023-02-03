import tensorflow as tf
import seaborn as sns
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.image as mpimg
import pickle
from sklearn.metrics import accuracy_score
from PIL import Image
from keras.datasets import mnist

mnist = tf.keras.datasets.mnist
(x_train, y_train), (x_test, y_test) = mnist.load_data()
img = x_train[4]


quad1 = img[14:28, 0:14]
quad2 = img[0:14, 0:14]
quad3 = img[0:14, 14:28]
quad4 = img[14:28, 14:28]
input_shape = (14, 14, 1)
x_train_quad1 = []
x_train_quad2 = []
x_train_quad3 = []
x_train_quad4 = []

x_test_quad1 = []
x_test_quad2 = []
x_test_quad3 = []
x_test_quad4 = []

for img in x_train:
  x_train_quad1.append(img[14:28, 0:14])
  x_train_quad2.append(img[0:14, 0:14])
  x_train_quad3.append(img[0:14, 14:28])
  x_train_quad4.append(img[14:28, 14:28])

for img in x_test:
  x_test_quad1.append(img[14:28, 0:14])
  x_test_quad2.append(img[0:14, 0:14])
  x_test_quad3.append(img[0:14, 14:28])
  x_test_quad4.append(img[14:28, 14:28])

x_train_quad1 = np.array(x_train_quad1)/255.0
x_train_quad2 = np.array(x_train_quad2)/255.0
x_train_quad3 = np.array(x_train_quad3)/255.0
x_train_quad4 = np.array(x_train_quad4)/255.0

x_test_quad1 = np.array(x_test_quad1)/255.0
x_test_quad2 = np.array(x_test_quad2)/255.0
x_test_quad3 = np.array(x_test_quad3)/255.0
x_test_quad4 = np.array(x_test_quad4)/255.0

# x_train=x_train.reshape(x_train.shape[0], x_train.shape[1], x_train.shape[2], 1)
# x_train=x_train / 255.0
# x_test = x_test.reshape(x_test.shape[0], x_test.shape[1], x_test.shape[2], 1)
# x_test=x_test/255.0

print(x_train_quad1.shape, x_test_quad1.shape)


idx = 23

quad1 = x_train_quad1[idx].reshape(14,14)
quad2 = x_train_quad2[idx].reshape(14,14)
quad3 = x_train_quad3[idx].reshape(14,14)
quad4 = x_train_quad4[idx].reshape(14,14)

y_train = tf.one_hot(y_train.astype(np.int32), depth=10)
y_test = tf.one_hot(y_test.astype(np.int32), depth=10)

batch_size = 64
num_classes = 10
epochs = 5

model1 = tf.keras.models.Sequential([
    tf.keras.layers.Conv2D(32, (5,5), padding='same', activation='relu', input_shape=input_shape),
    tf.keras.layers.Conv2D(32, (5,5), padding='same', activation='relu'),
    tf.keras.layers.MaxPool2D(),
    tf.keras.layers.Dropout(0.25),
    tf.keras.layers.Conv2D(64, (3,3), padding='same', activation='relu'),
    tf.keras.layers.Conv2D(64, (3,3), padding='same', activation='relu'),
    tf.keras.layers.MaxPool2D(strides=(2,2)),
    tf.keras.layers.Dropout(0.25),
    tf.keras.layers.Flatten(),
    tf.keras.layers.Dense(128, activation='relu'),
    tf.keras.layers.Dropout(0.5),
    tf.keras.layers.Dense(num_classes, activation='softmax')
])

model1.compile(optimizer=tf.keras.optimizers.RMSprop(epsilon=1e-08), loss='categorical_crossentropy', metrics=['acc'])

class myCallback(tf.keras.callbacks.Callback):
  def on_epoch_end(self, epoch, logs={}):
    if(logs.get('acc')>0.995):
      print("\nReached 99.5% accuracy so cancelling training!")
      self.model1.stop_training = True

callbacks = myCallback()

hist1 = model1.fit(x_train_quad1, y_train,
                    batch_size=batch_size,
                    epochs=epochs,
                    validation_split=0.1,
                    callbacks=[callbacks])

test_loss, test_acc = model1.evaluate(x_test_quad1, y_test)


converter = tf.lite.TFLiteConverter.from_keras_model(model1)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_model = converter.convert()

# Save the model.
with open('model1.tflite', 'wb') as f:
  f.write(tflite_model)   



model2 = tf.keras.models.Sequential([
    tf.keras.layers.Conv2D(32, (5,5), padding='same', activation='relu', input_shape=input_shape),
    tf.keras.layers.Conv2D(32, (5,5), padding='same', activation='relu'),
    tf.keras.layers.MaxPool2D(),
    tf.keras.layers.Dropout(0.25),
    tf.keras.layers.Conv2D(64, (3,3), padding='same', activation='relu'),
    tf.keras.layers.Conv2D(64, (3,3), padding='same', activation='relu'),
    tf.keras.layers.MaxPool2D(strides=(2,2)),
    tf.keras.layers.Dropout(0.25),
    tf.keras.layers.Flatten(),
    tf.keras.layers.Dense(128, activation='relu'),
    tf.keras.layers.Dropout(0.5),
    tf.keras.layers.Dense(num_classes, activation='softmax')
])

model2.compile(optimizer=tf.keras.optimizers.RMSprop(epsilon=1e-08), loss='categorical_crossentropy', metrics=['acc'])

class myCallback(tf.keras.callbacks.Callback):
  def on_epoch_end(self, epoch, logs={}):
    if(logs.get('acc')>0.995):
      print("\nReached 99.5% accuracy so cancelling training!")
      self.model2.stop_training = True

callbacks = myCallback()

hist2 = model2.fit(x_train_quad2, y_train,
                    batch_size=batch_size,
                    epochs=epochs,
                    validation_split=0.1,
                    callbacks=[callbacks])

test_loss, test_acc = model2.evaluate(x_test_quad2, y_test)

converter = tf.lite.TFLiteConverter.from_keras_model(model2)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_model = converter.convert()

# Save the model.
with open('model2.tflite', 'wb') as f:
  f.write(tflite_model)   

model3 = tf.keras.models.Sequential([
    tf.keras.layers.Conv2D(32, (5,5), padding='same', activation='relu', input_shape=input_shape),
    tf.keras.layers.Conv2D(32, (5,5), padding='same', activation='relu'),
    tf.keras.layers.MaxPool2D(),
    tf.keras.layers.Dropout(0.25),
    tf.keras.layers.Conv2D(64, (3,3), padding='same', activation='relu'),
    tf.keras.layers.Conv2D(64, (3,3), padding='same', activation='relu'),
    tf.keras.layers.MaxPool2D(strides=(2,2)),
    tf.keras.layers.Dropout(0.25),
    tf.keras.layers.Flatten(),
    tf.keras.layers.Dense(128, activation='relu'),
    tf.keras.layers.Dropout(0.5),
    tf.keras.layers.Dense(num_classes, activation='softmax')
])

model3.compile(optimizer=tf.keras.optimizers.RMSprop(epsilon=1e-08), loss='categorical_crossentropy', metrics=['acc'])

class myCallback(tf.keras.callbacks.Callback):
  def on_epoch_end(self, epoch, logs={}):
    if(logs.get('acc')>0.995):
      print("\nReached 99.5% accuracy so cancelling training!")
      self.model3.stop_training = True

callbacks = myCallback()

hist3 = model3.fit(x_train_quad3, y_train,
                    batch_size=batch_size,
                    epochs=epochs,
                    validation_split=0.1,
                    callbacks=[callbacks])

test_loss, test_acc = model3.evaluate(x_test_quad3, y_test)

converter = tf.lite.TFLiteConverter.from_keras_model(model3)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_model = converter.convert()

# Save the model.
with open('model3.tflite', 'wb') as f:
  f.write(tflite_model)   

model4 = tf.keras.models.Sequential([
    tf.keras.layers.Conv2D(32, (5,5), padding='same', activation='relu', input_shape=input_shape),
    tf.keras.layers.Conv2D(32, (5,5), padding='same', activation='relu'),
    tf.keras.layers.MaxPool2D(),
    tf.keras.layers.Dropout(0.25),
    tf.keras.layers.Conv2D(64, (3,3), padding='same', activation='relu'),
    tf.keras.layers.Conv2D(64, (3,3), padding='same', activation='relu'),
    tf.keras.layers.MaxPool2D(strides=(2,2)),
    tf.keras.layers.Dropout(0.25),
    tf.keras.layers.Flatten(),
    tf.keras.layers.Dense(128, activation='relu'),
    tf.keras.layers.Dropout(0.5),
    tf.keras.layers.Dense(num_classes, activation='softmax')
])

model4.compile(optimizer=tf.keras.optimizers.RMSprop(epsilon=1e-08), loss='categorical_crossentropy', metrics=['acc'])

class myCallback(tf.keras.callbacks.Callback):
  def on_epoch_end(self, epoch, logs={}):
    if(logs.get('acc')>0.995):
      print("\nReached 99.5% accuracy so cancelling training!")
      self.model4.stop_training = True

callbacks = myCallback()

hist4 = model4.fit(x_train_quad4, y_train,
                    batch_size=batch_size,
                    epochs=epochs,
                    validation_split=0.1,
                    callbacks=[callbacks])

test_loss, test_acc = model4.evaluate(x_test_quad4, y_test)

converter = tf.lite.TFLiteConverter.from_keras_model(model4)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_model = converter.convert()

# Save the model.
with open('model4.tflite', 'wb') as f:
  f.write(tflite_model)   