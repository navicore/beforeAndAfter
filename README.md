Before And After
================

###From the 11/16/2013 Google Android Hackathon

#### A meme generator of before and after sharable cameraphone pics
---------------------------------------------------------------

written in a hurry between 8:30AM and 4PM with rusty skills


1. Prompts the user to take a before pic and an after pic.
2. The pictures are then merged into a single image with BEFORE and AFTER captions.
3. The picture is in the Android Gallery and is sharable from within the app from the menubar.

That's it!

The code is a demo of

1. calling a camera via Intent
2. getting the images
3. displaying images
4. scaling images and drawaing them on another canvas
5. notifying Android to include the pics in a scan so that they show in 'Gallery'

Issues
------
1. the share menu item shows up but doesn't respond to touching


