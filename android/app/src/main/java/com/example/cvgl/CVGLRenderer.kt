package com.example.cvgl

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

    private var textureId: Int = 0
    private var mProgram: Int = 0
    private var mPositionHandle: Int = 0
    private var mTexCoordHandle: Int = 0
    private var mTextureUniformHandle: Int = 0
    
    private var mImageData: ByteArray? = null
    private var mImageWidth: Int = 0
    private var mImageHeight: Int = 0
    private val mDataLock = Any()

    private val vertexShaderCode =
        "attribute vec4 vPosition;" +
        "attribute vec2 a_TexCoordinate;" +
        "varying vec2 v_TexCoordinate;" +
        "void main() {" +
        "  gl_Position = vPosition;" +
        "  v_TexCoordinate = a_TexCoordinate;" +
        "}"

    private val fragmentShaderCode =
        "precision mediump float;" +
        "uniform sampler2D u_Texture;" +
        "varying vec2 v_TexCoordinate;" +
        "void main() {" +
        "  float c = texture2D(u_Texture, v_TexCoordinate).r;" + // Read Red component (grayscale)
        "  gl_FragColor = vec4(c, c, c, 1.0);" +
        "}"

    private val vertexBuffer = java.nio.ByteBuffer.allocateDirect(8 * 4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer().apply {
        put(floatArrayOf(
            -1.0f, 1.0f,   // Top Left
            -1.0f, -1.0f,  // Bottom Left
            1.0f, 1.0f,    // Top Right
            1.0f, -1.0f    // Bottom Right
        ))
        position(0)
    }

    private val texCoordBuffer = java.nio.ByteBuffer.allocateDirect(8 * 4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer().apply {
        put(floatArrayOf(
            0.0f, 0.0f, // Top Left (Rotated? Camera sensors are often rotated. We might need to adjust this)
            0.0f, 1.0f, // Bottom Left
            1.0f, 0.0f, // Top Right
            1.0f, 1.0f  // Bottom Right
        ))
        position(0)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        mProgram = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }
        
        // Generate texture
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        
        GLES20.glUseProgram(mProgram)
        
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        GLES20.glEnableVertexAttribArray(mPositionHandle)
        GLES20.glVertexAttribPointer(mPositionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        
        mTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "a_TexCoordinate")
        GLES20.glEnableVertexAttribArray(mTexCoordHandle)
        GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)
        
        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "u_Texture")
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(mTextureUniformHandle, 0)
        
        // Update texture if new data is available
        synchronized(mDataLock) {
            if (mImageData != null) {
                val buffer = java.nio.ByteBuffer.wrap(mImageData)
                // Use GL_LUMINANCE for single channel grayscale
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, mImageWidth, mImageHeight, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, buffer)
                mImageData = null // Clear after upload
            }
        }
        
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        
        GLES20.glDisableVertexAttribArray(mPositionHandle)
        GLES20.glDisableVertexAttribArray(mTexCoordHandle)
    }
    
    fun updateTexture(data: ByteArray, width: Int, height: Int) {
        synchronized(mDataLock) {
            mImageData = data
            mImageWidth = width
            mImageHeight = height
        }
    }
    
    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }
}
