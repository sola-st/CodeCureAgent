public class CameraService {
    public Camera prepareCamera(Camera camera, SceneCamera sceneCamera) {
        Camera theCamera = camera.duplicate();
        if (sceneCamera == null) {
            sceneCamera = new SceneCamera();
            sceneCamera.setDepthOfField(0.0);
            sceneCamera.setFocalDistance(theCamera.getDistToScreen());
        }
        return theCamera;
    }
}
