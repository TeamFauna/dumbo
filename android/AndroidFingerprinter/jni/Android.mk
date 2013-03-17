LOCAL_PATH := $(call my-dir)
 
include $(CLEAR_VARS)
 
LOCAL_MODULE    :=echoprint-jni
 
LOCAL_SRC_FILES :=AndroidCodegen.cpp \
            /echoprint-codegen/src/Codegen.cpp \
            /echoprint-codegen/src/Whitening.cpp \
            /echoprint-codegen/src/SubbandAnalysis.cpp \
            /echoprint-codegen/src/MatrixUtility.cpp \
            /echoprint-codegen/src/Fingerprint.cpp \
            /echoprint-codegen/src/Base64.cpp \
            /echoprint-codegen/src/AudioStreamInput.cpp \
            /echoprint-codegen/src/AudioBufferInput.cpp
 
LOCAL_LDLIBS    :=-llog\
        -lz
LOCAL_C_INCLUDES :=/Users/willhughes/Programming/local/Dumbo/android/AndroidFingerprinter/jni/echoprint-codegen/src \
            /Users/willhughes/Programming/local/Dumbo/android/AndroidFingerprinter/jni/boost_1_53_0               
 
include $(BUILD_SHARED_LIBRARY)
