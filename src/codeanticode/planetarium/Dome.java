/**
 * ##library.name##
 * ##library.sentence##
 * ##library.url##
 *
 * Copyright ##copyright## ##author##
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 * 
 * @author      ##author##
 * @modified    ##date##
 * @version     ##library.prettyVersion## (##library.version##)
 */

package codeanticode.planetarium;

import java.lang.reflect.Method;
import java.nio.IntBuffer;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PShape;
import processing.opengl.PGL;
import processing.opengl.PGraphics3D;
import processing.opengl.PShader;

/**
 * Basic dome renderer.
 * 
 */
public class Dome extends PGraphics3D {
  public final static String RENDERER = "codeanticode.planetarium.Dome";	
	public final static String VERSION  = "##library.prettyVersion##";
	
  protected PShader cubeMapShader;
  protected PShape domeSphere;
  protected PImage gridTex;

  protected IntBuffer cubeMapFbo;
  protected IntBuffer cubeMapRbo;
  protected IntBuffer cubeMapTex;

  protected boolean cubeMapInit = false;
  protected int cubeMapSize = 1024;
  
  protected int currentFace;
  
  
	/**
	 * The default constructor.
	 * 
	 */
	public Dome() {
		super();
		welcome();		
	}
	
	
  public void setParent(PApplet parent) {
    super.setParent(parent);
    
    Class<?> c = parent.getClass();
    Method method = null;
    try {
      method = c.getMethod("pre", new Class[] {});
    } catch (NoSuchMethodException nsme) {
    }
    if (method != null) {
      parent.registerMethod("pre", parent);
    }
  }
  
  
  public void setSize(int iwidth, int iheight) {
    super.setSize(iwidth, iheight);  
  }
  
	
  //////////////////////////////////////////////////////////////

  // All projection methods are disabled
	
  /*
  @Override
  public void ortho() {
    showMethodWarning("ortho");
  }


  @Override
  public void ortho(float left, float right,
                    float bottom, float top) {
    showMethodWarning("ortho");
  }


  @Override
  public void ortho(float left, float right,
                    float bottom, float top,
                    float near, float far) {
    showMethodWarning("ortho");
  }


  @Override
  public void perspective() {
    showMethodWarning("perspective");
  }


  @Override
  public void perspective(float fov, float aspect, float zNear, float zFar) {
    showMethodWarning("perspective");
  }


  @Override
  public void frustum(float left, float right, float bottom, float top,
                      float znear, float zfar) {
    showMethodWarning("frustum");
  }	

  
  @Override
  protected void defaultPerspective() {
    super.perspective();
  }
  */
  
  
  //////////////////////////////////////////////////////////////

  // Rendering
  
  
	public void beginDraw() {
	  super.beginDraw();
	  
	  if (0 < parent.frameCount) {	  
	    if (!cubeMapInit) {
	      initDome();
	    }
	    
	    beginPGL();
	    
	    // bind fbo
	    pgl.bindFramebuffer(PGL.FRAMEBUFFER, cubeMapFbo.get(0));   
	    
	    pgl.viewport(0, 0, cubeMapSize, cubeMapSize);    
	    super.perspective(90.0f * DEG_TO_RAD, 1.0f, 1.0f, 1025.0f);     
	    
	    beginFaceDraw(PGL.TEXTURE_CUBE_MAP_POSITIVE_X);
	  }
	}

	
  public void endDraw() {
    if (0 < parent.frameCount) {
      endFaceDraw();

      // Draw the rest of the cubemap faces
      for (int face = PGL.TEXTURE_CUBE_MAP_NEGATIVE_X; 
               face <= PGL.TEXTURE_CUBE_MAP_POSITIVE_Z; face++) {
        beginFaceDraw(face);    
        parent.draw();
        endFaceDraw();      
      }
      
      endPGL();
      
      renderDome(); 
    }    
    super.endDraw();    
  }
	
  
  private void initDome() {
    if (gridTex == null) {
      gridTex = parent.loadImage("grid.png");        
    }
    
    if (domeSphere == null) {
      sphereDetail(50);
      domeSphere = createShape(SPHERE, height/2.0f);
      domeSphere.setTexture(gridTex);
      domeSphere.rotateX(HALF_PI);
      domeSphere.setStroke(false);
    }
    
    if (cubeMapShader == null) {    
      cubeMapShader = parent.loadShader("cubemapfrag2.glsl", 
                                        "cubemapvert2.glsl"); 
      cubeMapShader.set("EnvMap", 1);
    }
    
    
    if (!cubeMapInit) {
      PGL pgl = beginPGL();
      
      cubeMapTex = IntBuffer.allocate(1);
      pgl.genTextures(1, cubeMapTex);
      pgl.bindTexture(PGL.TEXTURE_CUBE_MAP, cubeMapTex.get(0));
      pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_WRAP_S, 
                        PGL.CLAMP_TO_EDGE);
      pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_WRAP_T, 
                        PGL.CLAMP_TO_EDGE);
//      pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_WRAP_R, PGL.CLAMP_TO_EDGE);
      pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_MIN_FILTER, 
                        PGL.NEAREST);
      pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_MAG_FILTER, 
                        PGL.NEAREST);
      for (int i = PGL.TEXTURE_CUBE_MAP_POSITIVE_X; i < 
                   PGL.TEXTURE_CUBE_MAP_POSITIVE_X + 6; i++) {
        pgl.texImage2D(i, 0, PGL.RGBA8, cubeMapSize, cubeMapSize, 0, 
                       PGL.RGBA, PGL.UNSIGNED_BYTE, null);
      }
      
      // Init fbo, rbo
      cubeMapFbo = IntBuffer.allocate(1);
      cubeMapRbo = IntBuffer.allocate(1);
      pgl.genFramebuffers(1, cubeMapFbo);
      pgl.bindFramebuffer(PGL.FRAMEBUFFER, cubeMapFbo.get(0));
      pgl.framebufferTexture2D(PGL.FRAMEBUFFER, PGL.COLOR_ATTACHMENT0, 
                               PGL.TEXTURE_CUBE_MAP_POSITIVE_X, 
                               cubeMapTex.get(0), 0);

      pgl.genRenderbuffers(1, cubeMapRbo);
      pgl.bindRenderbuffer(PGL.RENDERBUFFER, cubeMapRbo.get(0));
      pgl.renderbufferStorage(PGL.RENDERBUFFER, PGL.DEPTH_COMPONENT24, 
                              cubeMapSize, cubeMapSize);
      
      // Attach depth buffer to FBO
      pgl.framebufferRenderbuffer(PGL.FRAMEBUFFER, PGL.DEPTH_ATTACHMENT, 
                                  PGL.RENDERBUFFER, cubeMapRbo.get(0));    

      pgl.enable(PGL.TEXTURE_CUBE_MAP);
      pgl.activeTexture(PGL.TEXTURE1);
      pgl.bindTexture(PGL.TEXTURE_CUBE_MAP, cubeMapTex.get(0));     
      
      endPGL();
      
      cubeMapInit = true;
    }
  }
  
  
  private void beginFaceDraw(int face) {
    currentFace = face; 
    
    resetMatrix();
    
    if (currentFace == PGL.TEXTURE_CUBE_MAP_POSITIVE_X) {
      camera(0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f);
    } else if (currentFace == PGL.TEXTURE_CUBE_MAP_NEGATIVE_X) {
      camera(0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f);
    } else if (currentFace == PGL.TEXTURE_CUBE_MAP_POSITIVE_Y) {
      camera(0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, 0.0f, -1.0f);  
    } else if (currentFace == PGL.TEXTURE_CUBE_MAP_NEGATIVE_Y) {
      camera(0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f);
    } else if (currentFace == PGL.TEXTURE_CUBE_MAP_POSITIVE_Z) {
      camera(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, -1.0f, 0.0f);    
    } else if (currentFace == PGL.TEXTURE_CUBE_MAP_NEGATIVE_Z) {
      camera(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f);
    }
    
    pgl.framebufferTexture2D(PGL.FRAMEBUFFER, PGL.COLOR_ATTACHMENT0, 
                             currentFace, cubeMapTex.get(0), 0);
  }
  
  
  private void endFaceDraw() {
    flush(); // Make sure that the geometry in the scene is pushed to the GPU
    pgl.framebufferTexture2D(PGL.FRAMEBUFFER, PGL.COLOR_ATTACHMENT0, 
                             currentFace, 0, 0);
  }
  
  
  private void renderDome() {
    ortho();
    resetMatrix();
    shader(cubeMapShader);
    shape(domeSphere);
    resetShader();
  }
  
  
  private void welcome() {
    System.out.println("##library.name## ##library.prettyVersion## by ##author##");
  }  
}

