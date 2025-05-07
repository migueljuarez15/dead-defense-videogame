package mygame;

import com.jme3.input.controls.*;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.Spatial;

public class JugadorControl extends AbstractControl implements ActionListener {

    private Camera cam;
    private Vector3f walkDirection = new Vector3f();
    private boolean left, right, forward, backward;
    private float speed = 5f;

    public JugadorControl(Camera cam) {
        this.cam = cam;
    }

    @Override
    protected void controlUpdate(float tpf) {
        Vector3f camDir = cam.getDirection().clone().setY(0).normalizeLocal();
        Vector3f camLeft = cam.getLeft().clone().setY(0).normalizeLocal();
        walkDirection.set(0, 0, 0);

        if (forward)  walkDirection.addLocal(camDir);
        if (backward) walkDirection.addLocal(camDir.negate());
        if (left)     walkDirection.addLocal(camLeft);
        if (right)    walkDirection.addLocal(camLeft.negate());

        spatial.move(walkDirection.mult(tpf * speed));
        cam.setLocation(spatial.getLocalTranslation().add(0, 2, 0));
    }

    @Override
    protected void controlRender(com.jme3.renderer.RenderManager rm, com.jme3.renderer.ViewPort vp) {}

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        switch (name) {
            case "Forward": forward = isPressed; break;
            case "Backward": backward = isPressed; break;
            case "Left": left = isPressed; break;
            case "Right": right = isPressed; break;
        }
    }
}