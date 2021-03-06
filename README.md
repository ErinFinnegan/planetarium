The planetarium library is intended to project 3D Processing sketches on spherical domes, with minimal changes in the code of the sketch.

It is based on the FullDomeTemplate code from Christopher Warnow, available at https://github.com/mphasize/FullDome

### Overview

A brief descrition of how it works: a 360° view of the scene is generated by rendering the scene 6 times from each direction: positive x, negative x, and so on. The output of each rendering is stored inside a cube map texture, which is then applied on a sphere representing the dome. Hence, the library calls the draw() method 6 times per frame in order to update the corresponding side of the cube map texture (in reality, only 5 times since the bottom side of the cube map is not invisible on the dome). 

So, it is important to keep in mind that if you need to perform some calculation only one time per frame, then the code for that calculation should be put inside the pre() method. Similarly, calculations that should performed only once after the rendering is concluded should be put in the post() method.

The library currently assumes that the the projector is placed in the center of the dome, the output will be incorrect if the projector is not centered. Multiple projectors are not currently supported.
