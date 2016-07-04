package org.jgltut.tut10;

import org.jglsdk.glutil.MousePoles.*;
import org.jgltut.Tutorial;
import org.jgltut.commons.ProjectionBlock;
import org.jgltut.commons.UnprojectionBlock;
import org.jgltut.framework.Framework;
import org.jgltut.framework.Mesh;
import org.jgltut.framework.MousePole;
import org.jgltut.framework.Timer;
import org.joml.*;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindBufferRange;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.GL_DEPTH_CLAMP;


/**
 * Visit https://github.com/integeruser/jgltut for info, updates and license terms.
 * <p>
 * Part III. Illumination
 * Chapter 10. Plane Lights
 * <p>
 * I,J,K,L  - control the light's position. Holding LEFT_SHIFT with these keys will move in smaller increments.
 * SPACE    - toggle between drawing the uncolored cylinder and the colored one.
 * O,U      - increase/decrease the attenuation constant.
 * Y        - toggle the drawing of the light source.
 * T        - toggle between the scaled and unscaled cylinder.
 * B        - toggle the light's rotation on/off.
 * H        - swap between the linear and quadratic interpolation functions.
 * <p>
 * LEFT   CLICKING and DRAGGING         - rotate the camera around the target point, both horizontally and vertically.
 * LEFT   CLICKING and DRAGGING + CTRL  - rotate the camera around the target point, either horizontally or vertically.
 * LEFT   CLICKING and DRAGGING + ALT   - change the camera's up direction.
 * RIGHT  CLICKING and DRAGGING         - rotate the object horizontally and vertically, relative to the current camera
 * view.
 * RIGHT  CLICKING and DRAGGING + CTRL  - rotate the object horizontally or vertically only, relative to the current
 * camera view.
 * RIGHT  CLICKING and DRAGGING + ALT   - spin the object.
 * WHEEL  SCROLLING                     - move the camera closer to it's target point or farther away.
 */
public class FragmentAttenuation extends Tutorial {
    public static void main(String[] args) {
        Framework.CURRENT_TUTORIAL_DATAPATH = "/org/jgltut/tut10/data/";
        new FragmentAttenuation().start(500, 500);
    }


    @Override
    protected void init() {
        initializePrograms();

        try {
            cylinderMesh = new Mesh("UnitCylinder.xml");
            planeMesh = new Mesh("LargePlane.xml");
            cubeMesh = new Mesh("UnitCube.xml");
        } catch (Exception exception) {
            exception.printStackTrace();
            System.exit(-1);
        }

        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CW);

        final float depthZNear = 0.0f;
        final float depthZFar = 1.0f;

        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);
        glDepthFunc(GL_LEQUAL);
        glDepthRange(depthZNear, depthZFar);
        glEnable(GL_DEPTH_CLAMP);

        projectionUniformBuffer = glGenBuffers();
        glBindBuffer(GL_UNIFORM_BUFFER, projectionUniformBuffer);
        glBufferData(GL_UNIFORM_BUFFER, ProjectionBlock.SIZE_IN_BYTES, GL_DYNAMIC_DRAW);

        unprojectionUniformBuffer = glGenBuffers();
        glBindBuffer(GL_UNIFORM_BUFFER, unprojectionUniformBuffer);
        glBufferData(GL_UNIFORM_BUFFER, UnprojectionBlock.SIZE_IN_BYTES, GL_DYNAMIC_DRAW);

        // Bind the static buffers.
        glBindBufferRange(GL_UNIFORM_BUFFER, projectionBlockIndex, projectionUniformBuffer, 0, ProjectionBlock.SIZE_IN_BYTES);

        // Bind the static buffers.
        glBindBufferRange(GL_UNIFORM_BUFFER, unprojectionBlockIndex, unprojectionUniformBuffer, 0, UnprojectionBlock.SIZE_IN_BYTES);

        glBindBuffer(GL_UNIFORM_BUFFER, 0);


        glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (action == GLFW_PRESS) {
                    switch (key) {
                        case GLFW_KEY_SPACE:
                            drawColoredCyl = !drawColoredCyl;
                            break;

                        case GLFW_KEY_O:
                            if (isKeyPressed(GLFW_KEY_LEFT_SHIFT) || isKeyPressed(GLFW_KEY_RIGHT_SHIFT)) {
                                lightAttenuation *= 1.1f;
                            } else {
                                lightAttenuation *= 1.5f;
                            }

                            System.out.printf("Atten: %f\n", lightAttenuation);
                            break;

                        case GLFW_KEY_U:
                            if (isKeyPressed(GLFW_KEY_LEFT_SHIFT) || isKeyPressed(GLFW_KEY_RIGHT_SHIFT)) {
                                lightAttenuation /= 1.1f;
                            } else {
                                lightAttenuation /= 1.5f;
                            }

                            System.out.printf("Atten: %f\n", lightAttenuation);
                            break;

                        case GLFW_KEY_Y:
                            drawLight = !drawLight;
                            break;

                        case GLFW_KEY_T:
                            scaleCyl = !scaleCyl;
                            break;

                        case GLFW_KEY_B:
                            lightTimer.togglePause();
                            break;

                        case GLFW_KEY_H:
                            useRSquare = !useRSquare;
                            if (useRSquare) {
                                System.out.printf("Inverse Squared Attenuation\n");
                            } else {
                                System.out.printf("Plain Inverse Attenuation\n");
                            }
                            break;

                        case GLFW_KEY_ESCAPE:
                            glfwSetWindowShouldClose(window, GL_TRUE);
                            break;
                    }
                }
            }
        });

        glfwSetMouseButtonCallback(window, mouseCallback = new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                boolean pressed = action == GLFW_PRESS;
                glfwGetCursorPos(window, mouseBuffer1, mouseBuffer2);
                int x = (int) mouseBuffer1.get(0);
                int y = (int) mouseBuffer2.get(0);
                MousePole.forwardMouseButton(window, viewPole, button, pressed, x, y);
                MousePole.forwardMouseButton(window, objtPole, button, pressed, x, y);
            }
        });
        glfwSetCursorPosCallback(window, mousePosCallback = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                if (isMouseButtonPressed(GLFW_MOUSE_BUTTON_LEFT) || isMouseButtonPressed(GLFW_MOUSE_BUTTON_RIGHT)) {
                    MousePole.forwardMouseMotion(viewPole, (int) xpos, (int) ypos);
                    MousePole.forwardMouseMotion(objtPole, (int) xpos, (int) ypos);
                }
            }
        });
        glfwSetScrollCallback(window, mouseScrollCallback = new GLFWScrollCallback() {
            @Override
            public void invoke(long window, double xoffset, double yoffset) {
                glfwGetCursorPos(window, mouseBuffer1, mouseBuffer2);
                int x = (int) mouseBuffer1.get(0);
                int y = (int) mouseBuffer2.get(0);
                MousePole.forwardMouseWheel(window, viewPole, (int) yoffset, x, y);
                MousePole.forwardMouseWheel(window, objtPole, (int) yoffset, x, y);
            }
        });
    }

    @Override
    protected void display() {
        lightTimer.update(elapsedTime);

        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        glClearDepth(1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        MatrixStackf modelMatrix = new MatrixStackf(10);
        modelMatrix.mul(viewPole.calcMatrix());

        final Vector4f worldLightPos = calcLightPosition();
        final Vector4f lightPosCameraSpace = modelMatrix.transform(new Vector4f(worldLightPos));

        glUseProgram(fragWhiteDiffuseColor.theProgram);
        glUniform4f(fragWhiteDiffuseColor.lightIntensityUnif, 0.8f, 0.8f, 0.8f, 1.0f);
        glUniform4f(fragWhiteDiffuseColor.ambientIntensityUnif, 0.2f, 0.2f, 0.2f, 1.0f);
        glUniform3fv(fragWhiteDiffuseColor.cameraSpaceLightPosUnif, lightPosCameraSpace.get(vec4Buffer));
        glUniform1f(fragWhiteDiffuseColor.lightAttenuationUnif, lightAttenuation);
        glUniform1i(fragWhiteDiffuseColor.useRSquareUnif, useRSquare ? 1 : 0);

        glUseProgram(fragVertexDiffuseColor.theProgram);
        glUniform4f(fragVertexDiffuseColor.lightIntensityUnif, 0.8f, 0.8f, 0.8f, 1.0f);
        glUniform4f(fragVertexDiffuseColor.ambientIntensityUnif, 0.2f, 0.2f, 0.2f, 1.0f);
        glUniform3fv(fragVertexDiffuseColor.cameraSpaceLightPosUnif, lightPosCameraSpace.get(vec4Buffer));
        glUniform1f(fragVertexDiffuseColor.lightAttenuationUnif, lightAttenuation);
        glUniform1i(fragVertexDiffuseColor.useRSquareUnif, useRSquare ? 1 : 0);
        glUseProgram(0);

        {
            modelMatrix.pushMatrix();

            // Render the ground plane.
            {
                modelMatrix.pushMatrix();

                Matrix3f normMatrix = new Matrix3f(modelMatrix);
                normMatrix.invert().transpose();

                glUseProgram(fragWhiteDiffuseColor.theProgram);
                glUniformMatrix4fv(fragWhiteDiffuseColor.modelToCameraMatrixUnif, false, modelMatrix.get(mat4Buffer));

                glUniformMatrix3fv(fragWhiteDiffuseColor.normalModelToCameraMatrixUnif, false, normMatrix.get(mat3Buffer));
                planeMesh.render();
                glUseProgram(0);

                modelMatrix.popMatrix();
            }

            // Render the Cylinder
            {
                modelMatrix.pushMatrix();

                modelMatrix.mul(objtPole.calcMatrix());

                if (scaleCyl) {
                    modelMatrix.scale(1.0f, 1.0f, 0.2f);
                }

                Matrix3f normMatrix = new Matrix3f(modelMatrix);
                normMatrix.invert().transpose();

                if (drawColoredCyl) {
                    glUseProgram(fragVertexDiffuseColor.theProgram);
                    glUniformMatrix4fv(fragVertexDiffuseColor.modelToCameraMatrixUnif, false, modelMatrix.get(mat4Buffer));

                    glUniformMatrix3fv(fragVertexDiffuseColor.normalModelToCameraMatrixUnif, false, normMatrix.get(mat3Buffer));
                    cylinderMesh.render("lit-color");
                } else {
                    glUseProgram(fragWhiteDiffuseColor.theProgram);
                    glUniformMatrix4fv(fragWhiteDiffuseColor.modelToCameraMatrixUnif, false, modelMatrix.get(mat4Buffer));

                    glUniformMatrix3fv(fragWhiteDiffuseColor.normalModelToCameraMatrixUnif, false, normMatrix.get(mat3Buffer));
                    cylinderMesh.render("lit");
                }
                glUseProgram(0);

                modelMatrix.popMatrix();
            }

            // Render the light
            if (drawLight) {
                modelMatrix.pushMatrix();

                modelMatrix.translate(worldLightPos.x, worldLightPos.y, worldLightPos.z);
                modelMatrix.scale(0.1f, 0.1f, 0.1f);

                glUseProgram(unlit.theProgram);
                glUniformMatrix4fv(unlit.modelToCameraMatrixUnif, false, modelMatrix.get(mat4Buffer));
                glUniform4f(unlit.objectColorUnif, 0.8078f, 0.8706f, 0.9922f, 1.0f);
                cubeMesh.render("flat");

                modelMatrix.popMatrix();
            }

            modelMatrix.popMatrix();
        }
    }

    @Override
    protected void reshape(int w, int h) {
        float zNear = 1.0f;
        float zFar = 1000.0f;
        Matrix4f persMatrix = new Matrix4f();
        persMatrix.perspective((float) Math.toRadians(45.0f), (w / (float) h), zNear, zFar);

        ProjectionBlock projData = new ProjectionBlock();
        projData.cameraToClipMatrix = persMatrix;

        UnprojectionBlock unprojData = new UnprojectionBlock();
        unprojData.clipToCameraMatrix = new Matrix4f(persMatrix).invert();
        unprojData.windowSize = new Vector2i(w, h);

        glBindBuffer(GL_UNIFORM_BUFFER, projectionUniformBuffer);
        glBufferSubData(GL_UNIFORM_BUFFER, 0, projData.getAndFlip(projectionBlockBuffer));
        glBindBuffer(GL_UNIFORM_BUFFER, unprojectionUniformBuffer);
        glBufferSubData(GL_UNIFORM_BUFFER, 0, unprojData.getAndFlip(unprojectionBlockBuffer));
        glBindBuffer(GL_UNIFORM_BUFFER, 0);

        glViewport(0, 0, w, h);
    }

    @Override
    protected void update() {
        final float scale = 5;

        if (isKeyPressed(GLFW_KEY_J)) {
            if (isKeyPressed(GLFW_KEY_LEFT_SHIFT) || isKeyPressed(GLFW_KEY_RIGHT_SHIFT)) {
                lightRadius -= 0.05f * lastFrameDuration * scale;
            } else {
                lightRadius -= 0.2f * lastFrameDuration * scale;
            }
        } else if (isKeyPressed(GLFW_KEY_L)) {
            if (isKeyPressed(GLFW_KEY_LEFT_SHIFT) || isKeyPressed(GLFW_KEY_RIGHT_SHIFT)) {
                lightRadius += 0.05f * lastFrameDuration * scale;
            } else {
                lightRadius += 0.2f * lastFrameDuration * scale;
            }
        }

        if (isKeyPressed(GLFW_KEY_I)) {
            if (isKeyPressed(GLFW_KEY_LEFT_SHIFT) || isKeyPressed(GLFW_KEY_RIGHT_SHIFT)) {
                lightHeight += 0.05f * lastFrameDuration * scale;
            } else {
                lightHeight += 0.2f * lastFrameDuration * scale;
            }
        } else if (isKeyPressed(GLFW_KEY_K)) {
            if (isKeyPressed(GLFW_KEY_LEFT_SHIFT) || isKeyPressed(GLFW_KEY_RIGHT_SHIFT)) {
                lightHeight -= 0.05f * lastFrameDuration * scale;
            } else {
                lightHeight -= 0.2f * lastFrameDuration * scale;
            }
        }


        if (lightRadius < 0.2f) {
            lightRadius = 0.2f;
        }

        if (lightAttenuation < 0.1f) {
            lightAttenuation = 0.1f;
        }
    }

    ////////////////////////////////
    private ProgramData fragWhiteDiffuseColor;
    private ProgramData fragVertexDiffuseColor;
    private UnlitProgData unlit;

    private class ProgramData {
        int theProgram;

        int modelToCameraMatrixUnif;

        int lightIntensityUnif;
        int ambientIntensityUnif;

        int normalModelToCameraMatrixUnif;
        int cameraSpaceLightPosUnif;
        int lightAttenuationUnif;
        int useRSquareUnif;
    }

    private class UnlitProgData {
        int theProgram;

        int objectColorUnif;
        int modelToCameraMatrixUnif;
    }


    private void initializePrograms() {
        fragWhiteDiffuseColor = loadLitProgram("FragLightAtten_PN.vert", "FragLightAtten.frag");
        fragVertexDiffuseColor = loadLitProgram("FragLightAtten_PCN.vert", "FragLightAtten.frag");
        unlit = loadUnlitProgram("PosTransform.vert", "UniformColor.frag");
    }

    private ProgramData loadLitProgram(String vertexShaderFileName, String fragmentShaderFileName) {
        ArrayList<Integer> shaderList = new ArrayList<>();
        shaderList.add(Framework.loadShader(GL_VERTEX_SHADER, vertexShaderFileName));
        shaderList.add(Framework.loadShader(GL_FRAGMENT_SHADER, fragmentShaderFileName));

        ProgramData data = new ProgramData();
        data.theProgram = Framework.createProgram(shaderList);
        data.modelToCameraMatrixUnif = glGetUniformLocation(data.theProgram, "modelToCameraMatrix");
        data.lightIntensityUnif = glGetUniformLocation(data.theProgram, "lightIntensity");
        data.ambientIntensityUnif = glGetUniformLocation(data.theProgram, "ambientIntensity");

        data.normalModelToCameraMatrixUnif = glGetUniformLocation(data.theProgram, "normalModelToCameraMatrix");
        data.cameraSpaceLightPosUnif = glGetUniformLocation(data.theProgram, "cameraSpaceLightPos");
        data.lightAttenuationUnif = glGetUniformLocation(data.theProgram, "lightAttenuation");
        data.useRSquareUnif = glGetUniformLocation(data.theProgram, "bUseRSquare");

        int projectionBlock = glGetUniformBlockIndex(data.theProgram, "Projection");
        glUniformBlockBinding(data.theProgram, projectionBlock, projectionBlockIndex);

        int unprojectionBlock = glGetUniformBlockIndex(data.theProgram, "UnProjection");
        glUniformBlockBinding(data.theProgram, unprojectionBlock, unprojectionBlockIndex);

        return data;
    }

    private UnlitProgData loadUnlitProgram(String vertexShaderFileName, String fragmentShaderFileName) {
        ArrayList<Integer> shaderList = new ArrayList<>();
        shaderList.add(Framework.loadShader(GL_VERTEX_SHADER, vertexShaderFileName));
        shaderList.add(Framework.loadShader(GL_FRAGMENT_SHADER, fragmentShaderFileName));

        UnlitProgData data = new UnlitProgData();
        data.theProgram = Framework.createProgram(shaderList);
        data.modelToCameraMatrixUnif = glGetUniformLocation(data.theProgram, "modelToCameraMatrix");
        data.objectColorUnif = glGetUniformLocation(data.theProgram, "objectColor");

        int projectionBlock = glGetUniformBlockIndex(data.theProgram, "Projection");
        glUniformBlockBinding(data.theProgram, projectionBlock, projectionBlockIndex);

        return data;
    }

    ////////////////////////////////
    private Mesh cylinderMesh;
    private Mesh planeMesh;
    private Mesh cubeMesh;

    private float lightHeight = 1.5f;
    private float lightRadius = 1.0f;
    private float lightAttenuation = 1.0f;
    private Timer lightTimer = new Timer(Timer.Type.LOOP, 5.0f);

    private boolean drawColoredCyl;
    private boolean drawLight;
    private boolean scaleCyl;
    private boolean useRSquare;


    private Vector4f calcLightPosition() {
        float currTimeThroughLoop = lightTimer.getAlpha();

        Vector4f lightPos = new Vector4f(0.0f, lightHeight, 0.0f, 1.0f);
        lightPos.x = (float) (Math.cos(currTimeThroughLoop * (3.14159f * 2.0f)) * lightRadius);
        lightPos.z = (float) (Math.sin(currTimeThroughLoop * (3.14159f * 2.0f)) * lightRadius);
        return lightPos;
    }

    ////////////////////////////////
    // View / Object setup.
    private ViewData initialViewData = new ViewData(
            new Vector3f(0.0f, 0.5f, 0.0f),
            new Quaternionf(0.3826834f, 0.0f, 0.0f, 0.92387953f),
            5.0f,
            0.0f
    );

    private ViewScale viewScale = new ViewScale(
            3.0f, 20.0f,
            1.5f, 0.5f,
            0.0f, 0.0f,  // No camera movement.
            90.0f / 250.0f
    );


    private ObjectData initialObjectData = new ObjectData(
            new Vector3f(0.0f, 0.5f, 0.0f),
            new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f)
    );


    private ViewPole viewPole = new ViewPole(initialViewData, viewScale, MouseButtons.MB_LEFT_BTN);
    private ObjectPole objtPole = new ObjectPole(initialObjectData, 90.0f / 250.0f, MouseButtons.MB_RIGHT_BTN, viewPole);

    ////////////////////////////////
    private final int projectionBlockIndex = 2;
    private final int unprojectionBlockIndex = 1;

    private int projectionUniformBuffer;
    private int unprojectionUniformBuffer;
}
