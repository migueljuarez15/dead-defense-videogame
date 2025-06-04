package mygame;

import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.input.controls.ActionListener;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.Spatial;

public class JugadorControl extends AbstractControl implements ActionListener {

    private int vida = 100;
    private boolean forward = false, backward = false, left = false, right = false;
    private float speed = 7.0f;
    private boolean estaMuerto = false;

    private final Camera cam;
    private final RigidBodyControl rigidBody;

    public JugadorControl(Camera cam, RigidBodyControl rigidBody) {
        this.cam = cam;
        this.rigidBody = rigidBody;
    }

    public int getVida() {
        return vida;
    }

    public void recibirDaño(int cantidad) {
        if (!estaMuerto) {
            vida -= cantidad;
            if (vida <= 0) {
                vida = 0;
                estaMuerto = true;
                // Lógica de muerte aquí (puedes detener el movimiento, mostrar mensaje, etc.)
            }
        }
    }

    public boolean estaMuerto() {
        return estaMuerto;
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (spatial == null || estaMuerto) return;

        Vector3f camDir = cam.getDirection().clone().setY(0).normalizeLocal();
        Vector3f camLeft = cam.getLeft().clone().setY(0).normalizeLocal();
        Vector3f walkDir = new Vector3f();

        if (forward)  walkDir.addLocal(camDir);
        if (backward) walkDir.addLocal(camDir.negate());
        if (left)     walkDir.addLocal(camLeft);
        if (right)    walkDir.addLocal(camLeft.negate());

        walkDir.normalizeLocal().multLocal(speed);

        rigidBody.setLinearVelocity(walkDir); // Aplicamos movimiento físico
        cam.setLocation(rigidBody.getPhysicsLocation().add(0, 2, 0));

        // Rotar el modelo hacia la dirección de la cámara
        float yaw = cam.getRotation().toAngles(null)[1];
        spatial.setLocalRotation(new Quaternion().fromAngles(0, yaw, 0));
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
