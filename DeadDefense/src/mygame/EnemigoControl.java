package mygame;

import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.*;
import com.jme3.math.*;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;

public class EnemigoControl extends AbstractControl {

    private Node path;
    private int currentIndex = 1;
    private float speed = 2f;

    public EnemigoControl(Node path) {
        this.path = path;
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (currentIndex >= path.getChildren().size()) return;

        Vector3f currentTarget = path.getChild(currentIndex).getLocalTranslation();
        Vector3f direction = currentTarget.subtract(spatial.getLocalTranslation()).normalizeLocal();
        spatial.move(direction.mult(tpf * speed));

        if (spatial.getLocalTranslation().distance(currentTarget) < 0.5f) {
            currentIndex++;
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}
}