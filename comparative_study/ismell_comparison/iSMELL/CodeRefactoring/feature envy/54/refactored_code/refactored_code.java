public synchronized void renderScene(Scene theScene,Camera camera,RenderListener rl,SceneCamera sceneCamera){
        RenderManager renderManager=new RenderManager(new CameraService(),new RenderSetupService());
        renderManager.renderScene(theScene,camera,rl,sceneCamera);
        }
