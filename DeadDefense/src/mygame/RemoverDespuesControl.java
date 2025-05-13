package mygame;

import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

public class RemoverDespuesControl extends AbstractControl {

    private float timer = 0;
    private float delay;
    private BulletAppState bulletAppState;

    public RemoverDespuesControl(float delay, BulletAppState bulletAppState) {
        this.delay = delay;
        this.bulletAppState = bulletAppState;
    }

    @Override
    protected void controlUpdate(float tpf) {
        timer += tpf;
        if (timer >= delay) {
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