package mygame;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.control.GhostControl;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.*;
import com.jme3.scene.control.AbstractControl;

public class EnemigoControl extends AbstractControl {

    private Node path;
    private int currentPoint = 0;
    private float speed = 2f;
    private boolean isDead = false;
    private float deathTimer = 0f;

    public EnemigoControl(Node path) {
        this.path = path;
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (isDead) {
            deathTimer += tpf;
            if (deathTimer >= 2f) {
                spatial.removeFromParent(); // eliminar enemigo
            }
            return;
        }

        if (path == null || path.getQuantity() == 0) return;

        Vector3f target = path.getChild(currentPoint).getLocalTranslation();
        Vector3f current = spatial.getLocalTranslation();
        Vector3f direction = target.subtract(current);

        if (direction.length() < 0.2f) {
            currentPoint++;
            if (currentPoint >= path.getQuantity()) {
                spatial.removeFromParent(); // llegó al final
                return;
            }
        } else {
            Vector3f move = direction.normalize().mult(speed * tpf);
            spatial.move(move);
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}

    public void die() {
        if (!isDead) {
            isDead = true;
            spatial.setLocalRotation(spatial.getLocalRotation().fromAngles((float)Math.toRadians(90), 0, 0));
        }
    }
}