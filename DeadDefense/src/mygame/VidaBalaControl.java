package mygame;

import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

/**
 * Dead Defense: El videojuego
 * @author Miguel Ángel Juárez Martínez
 * @author Franco Sánchez Gutierrez
 * @author Braulio Adrián Bollaín y Goytia Ortega
 */
public class VidaBalaControl extends AbstractControl {
    private float lifeTime;
    private final BulletAppState bulletAppState;

    public VidaBalaControl(float time, BulletAppState bulletAppState) {
        this.lifeTime = time;
        this.bulletAppState = bulletAppState;
    }

    @Override
    protected void controlUpdate(float tpf) {
        lifeTime -= tpf;
        if (lifeTime <= 0 && spatial != null) {
            RigidBodyControl control = spatial.getControl(RigidBodyControl.class);
            if (control != null) {
                bulletAppState.getPhysicsSpace().remove(control);
            }
            spatial.removeFromParent();
        }
    }

    @Override
    protected void controlRender(com.jme3.renderer.RenderManager rm, com.jme3.renderer.ViewPort vp) {}
}