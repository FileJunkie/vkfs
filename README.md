# vkfs
VKFS is a FUSE module for Linux system to access the user data in vk.com social network.

Currently it only supports downloading the images and renaming the galleries.

Usage:

`sbt 'run mount mountpoint userid'` to mount the galleries of user with userid=`userid` to the folder `mountpoint` in read-only mode.

`sbt 'run authorize'` to get an url to get you access token.

`sbt 'run mount mountpoint userid auth_token'` to mount in r/w mode with gallery renaming support.
