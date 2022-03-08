package com.github.moevm.adfmp1h22_player

import kotlin.math.min

import android.util.Log
import java.lang.Thread

import java.util.LinkedList
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.AudioTrack
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager

import org.eclipse.jetty.client.HttpClient
import java.lang.Exception
import java.nio.ByteBuffer

import android.os.Build
import android.app.NotificationManager
import android.app.NotificationChannel
import androidx.core.app.NotificationCompat
import android.app.Notification

import android.os.HandlerThread
import android.os.Handler
import android.os.Looper
import android.os.Message

import android.os.Binder
import android.os.IBinder
import android.content.Intent
import android.app.Service

// val BYTES = arrayOf<Byte>(-22, 73, -96, 117, -110, 32, -71, -120, -32, -26,
// -106, 34, 41, -123, 0, 0, 32, 8, 18, 3, -111, 61, -78, -26, 51, 88, -52,
// -108, -25, 84, 22, -11, 95, -12, 52, -112, -27, -102, -119, 54, 114,
// -54, 118, -118, 55, -43, 100, -94, -22, -32, 108, -93, -39, 55, 55, -82,
// -1, -58, 13, 30, 107, -99, 77, -9, -79, -101, 90, 84, 74, -1, 111, -69,
// -89, 71, -3, 63, -1, -20, -22, 0, -62, 0, 123, 9, -110, 23, 19, 79, 97,
// -20, -51, -15, 86, -24, 57, -63, -127, -33, 100, 32, -100, -73, 101,
// -104, 77, 123, -96, -59, 105, -22, -13, -8, -30, 10, 86, 14, 40, 17, 62,
// -89, -29, -111, -2, 8, 80, -116, -44, -86, 123, 22, 56, -67, -117, 85,
// 27, 77, 56, 10, 49, -100, 64, -15, 97, 87, 20, -69, 12, 64, -128, -48,
// 27, 98, -44, -111, 76, 124, -30, 17, -95, 78, 41, 106, -41, 75, 114,
// -90, 103, -30, 112, -120, -44, -80, -23, 48, -101, -41, 82, -29, -96,
// -127, 80, -8, -9, -91, 18, -44, -95, 96, -100, -54, -18, 77, -56, 1,
// -94, 97, 36, 0, 0, 0, -41, 35, -51, 61, 19, 2, -28, -98, 117, 31, 48,
// 98, -102, -62, -92, -46, -84, 56, 82, -50, 38, -106, 7, -124, 74, -31,
// -116, -103, -99, -61, -71, 7, 34, -27, 41, 6, 102, 6, -95, -62, 3, -44,
// -48, -17, 93, -98, -97, 71, -65, 119, -54, -24, -3, 63, -1, -110, 7, 33,
// 0, 0, 0, -44, 121, -128, 28, -64, -123, -116, -15, -90, 75, -32, 9, -89,
// -82, 82, -90, 66, 20, -58, -83, 60, 11, 2, -72, 113, -88, -63, -27, -80,
// 37, 87, 66, -21, -17, 44, 104, -16, 107, -25, -95, -90, -60, -89, -55,
// 64, 17, 16, -88, -128, 9, 8, 8, -11, -108, -112, 18, 52, 114, 82, 28,
// 19, 13, -122, 68, -124, -118, 35, -55, -102, 103, -86, -19, 119, 33,
// -55, -34, 89, -5, -16, 90, 49, 21, -87, -23, -107, -1, -5, -110, 100,
// -24, 6, 4, 14, 77, -38, 73, -20, 20, 112, 67, 68, -85, -118, 61, 38, 82,
// -114, 64, -15, 109, -116, 48, 111, 64, -4, 13, -18, -76, -13, 13, -118,
// -14, -101, 123, 8, 84, -46, 112, 111, -8, 64, -116, -103, 17, 64, 115,
// 119, -90, 35, -113, -127, 84, 74, 20, 46, -99, -98, 118, -102, -98, 45,
// -91, 126, 11, 51, -90, 15, -29, -112, -81, -4, 96, -27, -21, 78, 65,
// -51, 94, 41, -122, -25, 39, -7, 20, 0, 2, 72, -74, 12, 16, -115, -60,
// -118, 46, -43, -88, -36, 83, 62, 3, 103, 50, -73, -104, -79, 21, -66, 8,
// 32, -128, 90, -46, 45, 20, -86, -40, -104, -57, 74, -81, 71, -95, 115,
// -71, -35, 123, -48, -115, -33, -43, 53, -90, 126, 69, -69, 7, 109, -116,
// -64, 126, -18, -93, -23, -7, 15, -5, 63, -54, 89, -1, -99, 2, 100, 64,
// 0, 0, -29, -40, -40, 17, -43, 89, -30, 94, 20, -126, 44, 25, 103, -22,
// 17, 24, 1, -13, 60, 89, -44, 75, 80, -26, 50, 35, -50, 40, -49, 46, 100,
// -47, 36, -50, -93, 101, -100, -122, 123, -46, 0, 102, 114, 64, 70, -59,
// -30, 68, 93, 33, -59, 115, 15, -77, -119, 46, -52, 97, -65, 42, 115,
// -58, -2, -23, -40, -5, 103, 86, 84, 44, -87, 11, 40, -72, 99, 40, 101,
// -113, 43, -54, 103, -10, 85, 4, 101, 49, 86, -124, 96, 3, -122, -45, 32,
// 23, 42, -111, -94, -60, -45, 41, 35, -87, 97, -13, -21, -16, -96, -72,
// -44, -106, -60, 68, 2, 44, 46, 1, -86, -115, -56, 103, 3, 34, 64, -44,
// -66, 18, -31, 49, -87, 45, -62, 0, 34, 53, -3, -127, 26, -37, 69, 66,
// 43, 122, 12, -5, 103, 103, 122, -98, -54, -67, 67, 112, 69, 25, 10, -11,
// 69, 126, -43, 70, 91, 0, -49, -1, 35, -7, 92, -99, -72, 0, 0, 4, 72, 0,
// -96, -64, 42, 27, -101, -6, -77, -99, -91, 23, 121, -101, 86, -80, -5,
// -122, -118, -73, 40, 39, 25, 100, 98, 39, -110, -2, -65, 39, -82, 24,
// -125, 18, -40, -28, -128, -76, -100, 13, 56, -97, 82, 80, -30, 117, 11,
// 67, 73, 16, -126, -30, 21, -106, 50, 79, 90, 98, -72, -20, -78, 42, 55,
// 42, 75, -100, -5, 73, 86, 55, 73, -41, -23, 17, -49, -45, -11, -123, 80,
// -81, -121, -20, -57, -104, -51, -1, -5, -110, 100, -20, 2, 36, 98, 91,
// 89, -53, 9, 29, 80, 69, -60, -21, -102, 96, -61, 92, -114, 48, -17, 109,
// -121, -92, 113, -128, -20, -109, -18, 44, -61, 21, 120, -73, -40, -44,
// 59, -37, 32, 17, 65, 24, 6, 44, -71, 20, 116, 6, 36, -77, 60, 41, -76,
// -62, -81, 1, -122, -127, -42, -14, -55, -2, -15, -23, 29, -73, -66, -33,
// 79, -30, -11, 11, -21, 100, -75, -2, -115, -121, 40, 34, 46, -102, 86,
// -92, -108, 0, 0, 1, 125, 50, 102, 47, -88, 77, 34, -120, -3, 64, -101,
// 38, -127, 98, 110, -65, 12, -118, -96, 66, 112, -46, -60, 74, -97, 101,
// -73, 54, 23, 60, 71, -124, 49, 114, -103, 122, -110, 7, 95, 35, -80, 52,
// 33, 8, 53, 70, 73, 48, -57, 79, -69, -123, -93, -96, 95, -1, -2, 8, 127,
// -1, -14, -1, -6, -33, -1, 72, 22, -90, -23, 32, 0, 19, 73, -23, 8, -55,
// -68, -64, 28, -62, 2, -95, 46, -91, -127, -79, 80, 28, 1, -49, 9, -116,
// -50, 8, 25, -65, 36, -22, 31, -36, -103, -49, 15, 39, -54, -77, 71, 122,
// -67, -61, 90, 49, 80, -68, -124, 101, -75, -16, -22, -122, -72, 37, 47,
// 74, -24, -44, 75, -3, 91, 24, 96, 97, 3, -83, 100, 72, 25, 69, 48, 56,
// -124, 25, -67, 64, 64, 16, 88, 70, 109, -57, -42, 53, 96, 1, -81, -15,
// 16, 5, 13, 47, 66, 77, 80, 122, 9, 40, -44, -63, -82, -110, -53, -94,
// 90, 72, 47, 27, -93, 41, 78, -84, 88, 112, 54, 17, -95, 67, 4, 5, -41,
// -87, -122, 12, -128, -89, 74, 42, -24, -93, 104, -101, 101, -26, 77, 41,
// 106, -124, -46, 122, -47, -42, -117, 122, 89, -44, 110, -118, -47, -77,
// 72, 57, -88, -13, 77, -106, -22, 127, -106, 127, -1, -7, 63, -4, -113,
// -3, 106, 9, 1, 0, 0, 0, -40, 89, 108, 66, -77, -120, -80, 86, 30, -21,
// -70, 77, 53, -98, 81, -70, 75, 58, 2, -108, 21, 64, -16, -12, 85, 43,
// 29, 22, 32, -120, 41, 67, -119, 88, -100, 49, -64, 124, -88, 56, -86,
// 61, 36, -107, 11, 14, 96, -25, -86, 104, 48, -110, 35, 76, 98, 70, 96,
// 66, 7, -38, 35, 46, -31, -35, 72, -10, -84, -61, -34, 57, -120, 123,
// -61, 75, 115, 44, -33, -111, 43, -124, -26, -15, -39, 85, -17, -13, 64,
// -1, -5, -110, 100, -21, -128, 100, 82, 91, -39, 51, 44, 28, 112, 71,
// 102, 59, 122, 61, 34, 78, -115, 68, -65, 117, -89, -104, 113, 0, -6,
// -112, 46, 104, -12, -116, -6, 111, -79, -122, 123, -67, -67, 77, -48,
// -83, 95, -107, 63, -14, 58, -42, -123, 19, -11, -93, 113, 39, -9, -96,
// 102, -79, -33, -69, -8, -1, -111, -7, 0, -94, 0, 0, 0, 0, 0, 23, 122,
// 65, 82, -57, 89, -60, 87, 16, 81, -44, -126, 75, -80, 60, 100, -105, 32,
// -28, 86, -78, 42, 107, 4, -108, 104, 49, -121, 79, -94, 104, -68, -37,
// 84, 93, 30, 34, 21, 18, -95, 59, 59, 34, 85, 54, 88, 96, -120, 44, 96,
// 114, 33, -120, 4, 62, 86, 34, -118, 101, 71, -48, 111, 127, -2, -39,
// -81, -6, 127, -10, 127, -1, -36, 6, -128, 0, 4, -101, 104, -62, 33, 43,
// 67, 83, -17, 67, 93, 97, -51, 37, 104, 54, 116, 20, 27, 26, -7, -105,
// 55, 87, -42, 5, -83, 12, 50, 120, -62, -127, 112, -112, -112, -14, 20,
// 2, -103, 97, 104, -48, 73, 29, -112, -107, -84, 74, 118, 82, 52, 39,
// -71, 98, -47, 125, 97, -107, -91, 115, -119, 86, -85, -95, -127, 113,
// 99, -19, -31, -69, -116, -26, -81, -17, 3, -33, -28, 94, 98, 41, -96,
// -107, 71, -19, -122, -46, 2, -24, -41, 100, 80, 43, -3, -65, 47, -69,
// 109, 88, -17, -33, 101, -93, 90, -108, 116, -117, 74, 57, -64, 107,
// -123, 76, 72, -125, 39, 84, 72, 122, -102, 121, -59, 39, -101, 87, -66,
// -78, -116, 64, 34, 72, 0, -108, -36, -72, -61, 10, -117, -67, 47, -62,
// 54, 97, -93, 114, 63, 94, 38, -61, 47, -87, 91, -78, 76, 46, -4, -76,
// -40, 68, -42, -91, -22, -78, 41, -87, 17, 98, -14, -128, -34, 72, 114,
// -65, -4, -100, 97, -104, 71, 87, -85, -5, 33, -81, -112, -31, -33, -4,
// -9, -1, -3, -43, 32, 106, 1, 0, 0, 0, 90, 90, -84, -128, 105, 17, -119,
// -41, -126, -63, 12, 76, -74, -108, -84, 104, -82, 10, -111, 17, -98,
// 107, 36, -14, 58, 9, -11, -53, 3, -64, 108, -72, -1, 86, 70, 124, -90,
// -27, -28, -74, 40, -98, 90, -112, 31, 50, -20, 118, -94, -33, 93, -108,
// 79, 74, 24, -69, 66, -101, -6, -94, 126, 56, 98, -89, -66, 107, 89, -92,
// 38, -78, -77, 124, -91, -1, -5, -110, 100, -19, 2, 3, -39, 53, 90, 75,
// 12, 51, 82, 78, -60, -21, 77, 61, 35, 94, 16, 20, -3, 103, 44, 48, -47,
// 64, -1, -110, -82, -12, -12, -115, 42, 73, -92, 96, -19, -121, 42, 42,
// -31, 43, -123, -98, -49, 74, -104, -42, 111, -104, -19, 61, -37, 47,
// -17, -58, -7, -9, -18, -13, -92, 8, 53, 3, -119, -120, 28, -93, -92,
// -110, -73, 82, -32, 72, -88, 127, -85, 18, 119, 62, -94, -90, 110, 34,
// 0, 0, -110, -35, -77, 79, 107, -110, -36, -122, -120, 33, -72, 6, 88, 0,
// -93, -88, -59, 24, 60, 6, -36, -47, -27, 115, -124, 64, -120, -88, -74,
// 64, -113, 75, -61, -88, 120, -52, -39, -75, 12, -79, -51, -65, 117, 54,
// -105, 112, 119, -98, 117, 85, 93, 57, -5, -54, -55, 89, 8, -27, 111, -7,
// -49, 109, -65, -1, -5, -73, -2, 95, -1, -1, -1, 80, 96, 81, -6, -99, -1,
// -44, 56, -48, 46, 49, -120, 0, 16, -86, -68, 19, 34, -74, 57, 10, 39,
// 65, 65, 37, -59, -4, -71, 6, 41, 117, 40, 13, 2, -58, -69, 91, -74, -95,
// 54, -95, -63, -53, -40, 37, 42, 33, -124, 13, 38, -24, -34, 25, -78,
// 120, 116, 29, -96, -26, 68, -61, 51, -23, -127, -62, 43, 122, 79, -45,
// 111, -85, -38, 92, -101, 124, 34, 40, 115, -103, -12, -100, -52, -26,
// -18, 114, 52, 72, -74, -45, 62, 103, -36, -65, 60, -78, -1, 59, -7, -98,
// 103, 33, 111, -87, -97, 52, -65, -1, -68, 66, -90, -97, -117, 62, -47,
// -17, -16, -14, 45, -92, -97, -25, -24, 82, 64, 43, 2, -24, 122, -83, 66,
// 48, 77, -62, -36, 112, 23, -60, -94, 121, -28, -103, 32, -95, 46, 64,
// -64, -99, -100, -15, -60, 10, 57, -29, -80, -89, -121, 84, -86, -128,
// 26, 116, 100, -13, 60, 54, 31, 67, -114, -126, 2, -54, 87, -11, -111,
// -93, -1, -1, -1, -1, -102, 43, -82, 64, 0, 2, -51, -109, -120, 93, -61,
// 0, -32, 47, 35, -31, 12, 67, 5, -15, 62, 47, 42, -93, -103, 80, -96,
// 114, 122, -89, -76, 117, 57, 35, -63, -51, 71, 22, -54, -62, 58, -99,
// -107, 108, -108, -59, 23, -49, -86, 115, -12, -4, 65, 21, 4, -96, -53,
// -52, -70, -98, -24, -91, -66, 12, 118, -75, 74, -94, 100, -11, -50, 58,
// -103, -33, -74, -26, 127, -25, -103, -1, -5, -110, 100, -26, -126, 19,
// -12, 67, -38, 99, 12, 51, 112, 85, 41, -21, -83, 105, 5, 110, -114, 33,
// 111, 113, -89, -104, 113, 8, -23, -114, 46, 80, -12, -115, 40, -2, 89,
// -49, -71, -73, -106, 95, -97, -105, 14, 101, 44, 46, 75, 1, 43, 12, -98,
// -99, -73, -75, -76, -110, -30, -25, 54, 95, -40, -83, -12, 59, -3, -1,
// -35, 48, 0, 0, 18, -31, 25, 70, 14, 99, -119, -37, 25, 11, 73, 52, 42,
// -46, 33, -104, 11, -53, 36, 49, -44, 63, 72, -45, -87, 96, -128, -58,
// -58, 15, 54, 45, 34, 26, 113, 102, 64, -23, 72, -1, 52, 13, -52, 121,
// 54, -101, -107, -39, 11, 106, 8, -92, -102, -24, 17, 78, 127, 16, 51,
// 109, 95, -5, -10, 107, -37, 63, -21, -112, -1, -2, -113, -1, -19, -1,
// 36, 62, -98, -128, 4, 40, 67, -112, 18, 73, -1, 103, -117, 109, -64,
// -124, 57, 17, -42, 121, 2, -65, -74, -99, -89, 110, 85, 15, 20, 109,
// -51, -121, -59, -63, 68, -98, -103, 8, -120, -113, -22, -15, -45, 58,
// 26, -40, 35, -101, -111, 69, -84, -60, 72, -87, 105, -58, -54, 10, 92,
// -120, -121, 55, 58, 36, -14, -97, -114, 84, -12, -99, 21, 72, -91, 51,
// 44, -116, 82, -35, -107, -43, 25, -41, 4, -22, -92, 58, 101, -102, -22,
// -53, 84, 107, -77, 29, -98, 67, -51, -75, -37, 107, -46, 75, -5, 23, 38,
// 127, 117, 107, -123, 22, 79, 91, 26, 88, -54, 76, -70, 88, 39, 0, 40, 0,
// -94, -71, -63, 64, -99, 56, 17, 38, 3, 72, -107, -94, -115, -88, -125,
// -102, 2, 14, 20, -114, -110, 89, 23, 34, -32, -28, -106, 88, 54, -38,
// 49, 100, 75, -103, 56, 24, -94, 118, -69, -125, -103, 34, -54, -98, -18,
// 56, -80, 92, 30, 16, -30, -16, -49, -77, -4, 92, -67, 106, -37, -3, 31,
// 119, -1, -11, -86, 21, -60, 100, 0, 0, 0, 37, 58, -110, -61, 68, 80, 20,
// -112, 69, -122, 50, 28, 77, 88, -51, -126, 120, -32, -108, 107, -24,
// 105, 39, -117, -22, -34, -72, 76, -119, 70, 83, 78, 25, 44, 57, -76,
// -18, -114, 107, 84, -57, 56, -34, -58, -74, -47, -99, 77, -7, 53, -122,
// 108, 71, -31, 85, -84, -47, -107, -118, 28, 93, 69, -6, 31, 89, -38,
// -66, -14, 91, -35, -16, -2, 45, 50, -93, -34, 95, -1, -1, -5, -110, 100,
// -26, 2, 35, 124, 78, 92, -55, -26, 28, 66, 78, 100, -53, 71, 61, -119,
// 74, -114, -99, 89, 113, 44, 36, 79, -63, 17, -114, -83, -12, -106, 24,
// 40, -66, -115, 39, -83, 102, 111, 15, -84, 85, -2, -105, 48, 113, 102,
// -99, -1, -64, -85, -1, -1, -33, -10, 66, 0, 0, 0, 0, -100, -87, 34, 50,
// 78, -117, -23, 120, 116, 38, -118, 71, 38, -117, -97, 71, 33, -120, 70,
// 34, -58, 122, -105, 79, -99, 61, -47, -102, -44, -25, 72, -113, -112, 0,
// -127, 113, 64, 20, 101, -125, -122, 9, -108, 77, 88, 89, 2, 34, 117, 78,
// -88, -95, -12, 34, -109, -126, 23, -61, 19, 37, 50, -115, 124, -2, -23,
// 37, -115, -3, -65, -71, 95, -1, 87, -14, -33, -1, -6, -120, -127, -128,
// 1, -116, 97, 5, -27, -98, 52, -101, 111, -116, 101, -84, -47, 46, -44,
// -85, 107, -109, 23, 82, 76, -119, 80, 68, 69, 105, 104, -109, 36, -114,
// -10, -23, 78, -107, 108, 39, 106, 114, 33, -91, -30, -14, 37, 86, -81,
// 82, 35, 88, -103, 93, -63, 126, -64, -57, -90, 48, 125, 1, -110, 32,
// -79, -31, 78, -29, 81, 72, -125, 2, -54, 93, 112, -27, 71, 118, 96, 83,
// -83, 20, -18, -21, -21, 47, -19, -92, -20, 89, 124, -28, 53, 72, -9, 75,
// 50, -117, -88, -4, -2, 27, 54, -112, 104, 114, -91, -84, 62, -116, 52,
// 56, -96, 49, -100, 1, 14, 69, 43, -72, 121, -59, -70, -73, 68, -82, 17,
// 3, 34, 0, 18, -111, 92, 121, 59, 81, 10, 18, 116, 24, -25, -24, -122,
// -93, 76, -92, 40, 88, 53, 16, 10, 74, -100, -51, -103, -116, -49, 35,
// 32, 109, 53, 5, 68, -127, -32, -58, 46, 29, 12, 120, -108, -120, 97, 86,
// 101, -112, -59, 30, 84, -86, 18, 65, 26, 103, -14, -95, -31, -72, -88,
// 13, 69, -3, -97, -1, -108, 119, -39, -1, 127, -45, -12, -94, 11, 20,
// 100, 32, 0, 0, 36, -70, -56, 21, 104, 104, -79, -74, 6, -110, 76, -93,
// 41, -98, -117, 69, -45, 105, -59, 58, 116, -24, 79, 50, -80, 56, 43,
// -36, 91, -43, -116, 9, -27, 99, 99, 2, 20, 12, 42, -42, -63, -117, 67,
// 78, -25, 78, 18, 51, -110, -123, 49, 83, 76, -120, 30, -8, -29, -115,
// -108, -102, -92, 42, 116, 97, -94)

// TODO
// - HTTP
//   - Make requests
//   - ContentType
// - Buffers
//   - Skip and save metadata
//   - Parse MP3 frames
// - Audio
//   - MediaCodec
//   - AudioTrack (basically copy the docs)

private const val CMD_START_PLAYING_STATION = 0
private const val CMD_STOP_PLAYBACK = 1
// val CMD_PAUSE_PLAYBACK = 2
// val CMD_RESUME_PLAYBACK = 3

class PlayerService : Service() {

    val NOTIF_CHANNEL_ID = "main"
    val NOTIFY_ID = 1

    var mThread: PlayerThread? = null
    var mHandler: Handler? = null

    class PlayerThread(
        private val userAgent: String
    ) : HandlerThread("PlayerThread") {

        // TODO #4
        // 1. Set up a MediaCodec
        // 2. Set up an AudioTrack
        // 3. Feed frames through the thing

        lateinit var hc: HttpClient

        var handler: Handler? = null
        var sid: Int? = null

        var metaint: Int? = null
        var content_type: String? = null
        var decoder: DecoderFSM? = null

        // TODO
        // 1. Allocate buffer in onFormat
        // 2. Record bytes in onPayload
        // 3. Save into bqueue in onFrameDone
        // 4. Copy all queued buffers in onIBA

        val bqueue = LinkedList<ByteBuffer>()
        val freelist = LinkedList<ByteBuffer>()
        var current_buffer: ByteBuffer? = null

        var decoder_codec: MediaCodec? = null
        var sample_rate: Int = -1
        var timestamp: Long = 0.toLong()
        var player: AudioTrack? = null

        var counter: Int = 0

        fun reset() {
            metaint = null
            content_type = null
            decoder = null
        }

        private fun setupPlayer(fmt: MediaFormat) {
            val chcfg = when (fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)) {
                1 -> AudioFormat.CHANNEL_OUT_MONO
                2 -> AudioFormat.CHANNEL_OUT_STEREO
                else -> AudioFormat.CHANNEL_OUT_STEREO // idk
            }
            val enc = fmt.getInteger(MediaFormat.KEY_PCM_ENCODING)
            val fmt2 = AudioFormat.Builder()
                .setSampleRate(sample_rate)
                .setEncoding(enc)
                .setChannelMask(chcfg)
                .build()
            val at = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(fmt2)
                .setBufferSizeInBytes(
                    AudioTrack.getMinBufferSize(sample_rate, chcfg, enc)
                )
                .setSessionId(sid!!)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            val state = at.getState()
            when (state) {
                AudioTrack.STATE_INITIALIZED -> {
                    Log.d("APPDEBUG", "player init ok")
                }
                else -> {
                    Log.d("APPDEBUG", "player init NOT OK but $state")
                }
            }

            Log.d("APPDEBUG", "player stream type ${at.getStreamType()}")
            Log.d("APPDEBUG", "player play state ${at.getPlayState()}")
            Log.d("APPDEBUG", "player routed to ${at.getRoutedDevice()}")

            player?.release()
            player = at
        }

        private fun setupDecoderCodec(
            ctype: String,
            freq: Int,
            channels: Int,
        ): Boolean {
            return when (ctype) {
                "audio/mpeg" -> {
                    val mc = MediaCodec.createDecoderByType("audio/mpeg")
                    val fmt = MediaFormat.createAudioFormat(
                        "audio/mpeg",
                        freq, channels,
                    )
                    mc.setCallback(
                        object : MediaCodec.Callback() {
                            override fun onError(
                                mc: MediaCodec,
                                e: MediaCodec.CodecException,
                            ) {

                                Log.d("APPDEBUG", "MediaCodec: onError ${e.toString()}", e)

                            }

                            override fun onInputBufferAvailable(
                                mc: MediaCodec,
                                index: Int,
                            ) {

                                // Log.d("APPDEBUG", "onIBA $index")

                                val buf = mc.getInputBuffer(index)
                                if (buf == null) {
                                    Log.d("APPDEBUG", "no promised input buffer $index")
                                    return
                                }

                                buf.clear()
                                // Log.d("APPDEBUG", "onIBA buf $buf ${bqueue.size}")

                                if (!bqueue.isEmpty()) {
                                    val inb = bqueue.removeFirst()
                                    // Log.d("APPDEBUG", "${inb.toString()} ${inb.array().size}")
                                    // inb.limit(inb.position())
                                    // inb.position(0)
                                    if (inb.remaining() <= buf.remaining()) {
                                        // Log.d("APPDEBUG", "to buf - " + inb.array().toList().toString())
                                        buf.put(inb)
                                        inb.clear()
                                        freelist.addLast(inb)
                                    }
                                } else {
                                    // Log.d("APPDEBUG", "bqueue empty")
                                }

                                // while (!bqueue.isEmpty()) {
                                //     val inb = bqueue.removeFirst()
                                //     if (inb.remaining() > buf.remaining()) {
                                //         break
                                //     }
                                //     buf.put(inb)
                                //     inb.clear()
                                //     freelist.addLast(inb)
                                // }

                                val n = buf.position()
                                buf.rewind()

                                // Log.d("APPDEBUG", "qIB $n $index $buf $timestamp")

                                mc.queueInputBuffer(index, 0, n, timestamp, 0)

                                timestamp += 1152000000 / sample_rate

                            }

                            override fun onOutputBufferAvailable(
                                mc: MediaCodec,
                                index: Int,
                                info: MediaCodec.BufferInfo,
                            ) {
                                Log.d("APPDEBUG", "output available")

                                val buf = mc.getOutputBuffer(index)
                                if (buf == null) {
                                    Log.d("APPDEBUG", "no promised output buffer $index")
                                    return
                                }

                                Log.d("APPDEBUG", "recv buffer ${buf.remaining()}")

                                player?.let { at ->
                                    if (at.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                                        Log.d("APPDEBUG", "starting playback")
                                        at.play()
                                    }
                                    Log.d("APPDEBUG", "sending buffer")
                                    at.write(buf, buf.remaining(), AudioTrack.WRITE_BLOCKING)
                                }

                                mc.releaseOutputBuffer(index, false)
                            }

                            override fun onOutputFormatChanged(
                                mc: MediaCodec,
                                fmt: MediaFormat,
                            ) {
                                Log.d("APPDEBUG", "MediaCodec: onOutputFormatChanged ${fmt.toString()}")
                                setupPlayer(fmt)
                            }
                        },
                        handler!!
                    )
                    mc.configure(fmt, null, null, 0)
                    mc.start()
                    decoder_codec = mc
                    true
                }
                else -> false
            }
        }

        private fun setupRequest(url: String) {
            reset()
            hc.newRequest(url)
                .header("icy-metadata", "1")
                .agent(userAgent)
                .onResponseHeader { _, f ->
                    val v = f.getValue()
                    when (f.getLowerCaseName()) {
                        "icy-metaint" ->
                            metaint = v.toIntOrNull()
                        "content-type" -> {
                            content_type = v
                            Log.d("APPDEBUG", "content-type $v")
                        }
                        "icy-br" ->
                            Log.d("APPDEBUG", "bitrate $v")
                        "icy-name" ->
                            Log.d("APPDEBUG", "station name $v")
                        "server" ->
                            Log.d("APPDEBUG", "station srv $v")
                    }
                    true
                }
                .onResponseHeaders { r ->
                    var fsm: DecoderFSM = when (content_type) {
                        "audio/mpeg" -> Mp3HeaderDecoderFSM(
                            object : Mp3HeaderDecoderFSM.Callback {
                                override fun onFormat(
                                    frame_len: Int, freq_hz: Int,
                                    mode: Mp3HeaderDecoderFSM.Mode,
                                ) {
                                    handler!!.post {
                                        // Log.d("APPDEBUG", "onF")

                                        if (decoder_codec == null) {
                                            Log.d("APPDEBUG", "setup decoder codec")
                                            setupDecoderCodec(
                                                content_type!!,
                                                freq_hz,
                                                mode.channelsCount(),
                                            )
                                        }
                                        sample_rate = freq_hz

                                        // if (player == null) {
                                        //     decoder_codec?.let {
                                        //         setupPlayer(it.getOutputFormat())
                                        //     }
                                        // }

                                        var buf: ByteBuffer? = null
                                        while (!freelist.isEmpty()) {
                                            val b = freelist.removeFirst()
                                            if (b.capacity() >= frame_len) {
                                                break
                                            }
                                        }
                                        if (buf == null) {
                                            buf = ByteBuffer.allocate(frame_len * 3 / 2)
                                        }

                                        // Log.d("APPDEBUG", "set buffer $buf")

                                        if (buf == null) {
                                            Log.d("APPDEBUG", "no buffer allocated")
                                        }
                                        if (buf?.position() != 0) {
                                            Log.d("APPDEBUG", "dirty buffer")
                                        }

                                        // Log.d("BUFFER", "new buf $buf")
                                        current_buffer = buf
                                    }
                                }

                                override fun onPayload(c: ByteBuffer) {

                                    // val c2 = ByteBuffer.allocate(c.remaining())
                                    // c2.put(c)
                                    // c.rewind()
                                    // Log.d("APPDEBUG", "payload - " + c2.array().toList().toString())

                                    // TODO HOW TO FIX
                                    // If moving execution to another thread, COPY the buffer
                                    // Otherwise, SYNCHRONIZE around current_buffer, or bqueue, or freelist

                                    val cc = ByteBuffer.allocate(c.remaining())
                                    cc.put(c)
                                    cc.rewind()

                                    handler!!.post {
                                        // Log.d("APPDEBUG", "onP")

                                        current_buffer?.let { buf ->
                                            if (cc.remaining() < buf.remaining()) {

                                                // Log.d("BUFFER", "put ${cc.remaining()} to $buf")
                                                // if (cc.remaining() == 4) {
                                                //     Log.d("BUFFER", "short " + cc.array().toList().toString())
                                                // }

                                                buf.put(cc)

                                                // Log.d("APPDEBUG", "after put - " +
                                                //       buf.array().slice(0 until buf.position()).toList().toString())
                                        // c.rewind()
                                        // val c3 = ByteBuffer.allocate(c.remaining())
                                        // c3.put(c)
                                        // c.rewind()
                                        // Log.d("APPDEBUG", "put - ${c.remaining()} " + c3.array().toList().toString())

                                        //         val bp = buf.position()
                                        //         buf.position(0)
                                        // val c2 = ByteBuffer.allocate(buf.remaining())
                                        // c2.put(buf)
                                        // // buf.rewind()
                                        // Log.d("APPDEBUG", "buf - $bp " + c2.array().toList().toString())
                                        //         buf.position(bp)

                                            }
                                        }
                                    }

                                }

                                override fun onFrameDone() {
                                    handler!!.post {

                                        current_buffer?.let {

                                            // Log.d("APPDEBUG", "onFD")

                                    // val c2 = ByteBuffer.allocate(it.remaining())
                                    // c2.put(it)
                                    // it.rewind()
                                    // Log.d("APPDEBUG", "frame done - " + c2.array().toList().toString())

                                    val n = it.position()
                                    if (n > 0) {

                                        // Log.d("BUFFER", "done $it")
                                        // Log.d("XBYTES", it.array().slice(0 until it.position()).toList().toString())

                                            it.limit(it.position())
                                            it.rewind()

                                            bqueue.addLast(it)
                                            current_buffer = null
                                    } else {
                                        // Log.d("BUFFER", "empty $it")
                                        freelist.addLast(it)
                                    }
                                        }

                                    }
                                }
                            }
                        )
                        else -> {
                            Log.d("APPDEBUG", "aborting, content-type: $content_type")
                            r.abort(Exception("Unsupported format $content_type"))
                            return@onResponseHeaders
                        }
                    }

                    metaint?.let {
                        val fsm1 = fsm
                        fsm = IcyMetaDataDecoderFSM(
                            it, object : IcyMetaDataDecoderFSM.Callback {
                                override fun onPayload(c: ByteBuffer) {
                                    //Log.d("APPDEBUG", "icy -> mp3")
                                    fsm1.step(c)
                                }

                                override fun onMetaData(s: String) {
                                    //Log.d("APPDEBUG", "metadata: $s")
                                }
                            }
                        )
                    }

                    decoder = fsm
                }
                .onResponseContent { _, c ->
                    // TODO: try event loop mode?

                    // val bc = ByteBuffer.allocate(
                    //     c.remaining())
                    // bc.put(c)
                    // bc.rewind()
                    // handler!!.post {
                        //Log.d("APPDEBUG", "posted stuff runs")
                        // decoder?.step(bc)
                    // }

                    // val b = ByteBuffer.allocate(c.remaining())
                    // b.put(c)
                    // c.rewind()
                    // var i = 0
                    // b.rewind()
                    // while (i < b.remaining()) {
                    //     val e = min(i+16, b.remaining())
                    //     Log.d("APPDEBUG", "recv -/ " + b.array().slice(i until e).toList().toString())
                    //     i = e
                    // }

                    // if (counter != 0) {
                    //     // looper.quitSafely()
                    //     return@onResponseContent
                    // }

                    // if (counter > 10000) {
                    //     return@onResponseContent
                    // }

                    // val n = c.remaining()
                    // val n = BYTES.size
                    // val b = ByteBuffer.allocate(n)
                    // b.put(BYTES.slice(counter until (counter + n)).toByteArray())
                    // counter += n
                    // b.rewind()

                    // Log.d("APPDEBUG", "recv - "
                    //       + b.array().slice(0 until b.position()).toList().toString())

                    // counter += c.remaining()
                    // Log.d("APPDEBUG", "counter $counter")
                    // decoder?.step(c)

                    // decoder!!.step(b)
                    decoder!!.step(c)

                                    // ++counter
                                    // if (counter == 3) {
                                        // looper.quitSafely()
                                    // }
                }
                .send { r ->
                    //Log.d("APPDEBUG", "done")
                    handler!!.post {
                        player?.release()
                        decoder_codec?.release()
                        if (r.isFailed()) {
                            try {
                                Log.d("APPDEBUG", "failed")
                                r.getRequestFailure()?.let {
                                    Log.d("APPDEBUG", "req  fail: ${it.toString()}", it)
                                }
                                r.getResponseFailure()?.let {
                                    Log.d("APPDEBUG", "resp fail: ${it.toString()}", it)
                                }
                            } catch (e: Exception) {
                                Log.d("APPDEBUG", "exception in result")
                            }
                        }
                    }
                }
        }

        fun handleMessage(msg: Message): Boolean {
            return when (msg.what) {
                CMD_START_PLAYING_STATION -> {
                    val url = msg.obj as String
                    //Log.d("APPDEBUG", "start $url")
                    setupRequest(url)
                    true
                }
                CMD_STOP_PLAYBACK -> {
                    //Log.d("APPDEBUG", "stop")
                    true
                }
                else -> false
            }
        }

        override fun run() {
            Log.d("APPDEBUG", "thread starting")
            hc = HttpClient()
            hc.start()
            Log.d("APPDEBUG", "looper starting")
            super.run()
            Log.d("APPDEBUG", "looper exited")
            hc.stop()
            Log.d("APPDEBUG", "thread stopping")
        }
    }

    fun startPlayingStation(s: Station) {
        mHandler?.let {
            it.obtainMessage(
                CMD_START_PLAYING_STATION,
                s.streamUrl,
            ).sendToTarget()
        }
    }

    fun stopPlayback() {
        mHandler?.let {
            it.obtainMessage(CMD_STOP_PLAYBACK)
                .sendToTarget()
        }
        stopSelf()
    }

    inner class PlayerServiceBinder : Binder() {
        val service: PlayerService
            get () = this@PlayerService
    }

    override fun onStartCommand(intent: Intent, flags: Int, id: Int): Int {
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = PlayerServiceBinder()

    private fun makeNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                NOTIF_CHANNEL_ID,
                "Default",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(chan)
            NotificationCompat.Builder(this, chan.id)
        } else {
            NotificationCompat.Builder(this)
        }

        val notif = builder
            .setSmallIcon(R.drawable.ic_note)
            .setContentTitle("Radio Player")
            .setContentText("Service is running")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        return notif
    }

    override fun onCreate() {
        val am = getSystemService(AudioManager::class.java)
        val sid = am.generateAudioSessionId()

        mThread = PlayerThread(resources.getString(R.string.user_agent))
            .also { thread ->
                 thread.start()
                 val h = Handler(thread.looper, thread::handleMessage)
                 mHandler = h
                 thread.handler = h // NOTE: there’s probably a race here
                 thread.sid = sid
            }

        val notif = makeNotification()
        startForeground(NOTIFY_ID, notif)
    }

    override fun onDestroy() {
        mThread?.let {
            it.looper.quitSafely()
            it.join()
            mThread = null
        }
    }
}
