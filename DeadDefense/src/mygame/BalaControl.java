package mygame;

import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.Spatial;
import com.jme3.bullet.BulletAppState;

/**
 * Dead Defense: El videojuego
 * @author Miguel Ángel Juárez Martínez
 * @author Franco Sánchez Gutierrez
 * @author Braulio Adrián Bollaín y Goytia Ortega
 */
public class BalaControl extends AbstractControl {
    private float lifetime = 0;
    private final float maxLifetime = 2f; // segundos
    private BulletAppState bulletAppState;

    public BalaControl(BulletAppState bulletAppState) {
        this.bulletAppState = bulletAppState;
    }

    @Override
    protected void controlUpdate(float tpf) {
        lifetime += tpf;

        // Si supera el tiempo de vida, se elimina
        if (lifetime >= maxLifetime && spatial != null) {
            RigidBodyControl rbc = spatial.getControl(RigidBodyControl.class);
            if (rbc != null) {
                bulletAppState.getPhysicsSpace().remove(rbc);
            }
            spatial.removeFromParent();
        }
    }

    @Override
    protected void controlRender(com.jme3.renderer.RenderManager rm, com.jme3.renderer.ViewPort vp) {}
}