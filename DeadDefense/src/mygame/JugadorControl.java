package mygame;

import com.jme3.input.controls.*;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.Spatial;

public class JugadorControl extends AbstractControl implements ActionListener {
    private int vida = 100;
    private boolean forward = false;
    private boolean backward = false;
    private boolean left = false;
    private boolean right = false;
    private Vector3f walkDirection = new Vector3f();
    private float speed = 5.0f;
    private Camera cam;
    private boolean estaMuerto = false;

    public JugadorControl(Camera cam) {
        this.cam = cam;
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
                // Aquí podrías manejar la muerte del jugador
            }
        }
    }
    
    public boolean estaMuerto() {
        return estaMuerto;
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

        // === ROTAR el modelo para que mire en la dirección de la cámara ===
        float yaw = cam.getRotation().toAngles(null)[1]; // Obtener rotación en Y
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
