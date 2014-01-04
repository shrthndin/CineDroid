CineDroid
=========
This was an attempt at developing a better video camera app for Android devices for more "cinematic" control over camera parameters.
Due to the variety of Android camera drivers (even across mainstream devices) this became a "more than arduous" task for my interest, so my focus (no pun) in this project has waned. Main issues were selecting the frame rate of the camera. A possible workaround for devices that support 
a solid 30/60fps rate would be capturing raw YUV frames at a selected rate and sending the frames to a custom ffmpeg codec in a C library.
