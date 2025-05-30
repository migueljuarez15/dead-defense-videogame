package mygame;

import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.bullet.control.GhostControl;
import com.jme3.math.Quaternion;


public class EnemigoControl extends AbstractControl {
    
    // Variables mejoradas
    private int currentWaypoint = 0;  // Punto actual en el camino
    private float speed = 2.0f;       // Velocidad base
    private boolean isDead = false;   // Estado del enemigo
    private float deathTimer = 0f;    // Temporizador para eliminación
    private float rotationSpeed = 4f; // Velocidad de rotación suavizada
    private Spatial target; // El jugador
    
    // Nueva variable para variación de velocidad
    private float speedVariation = 0f; 

    public EnemigoControl(Spatial target) {
        this.target = target;
        this.speedVariation = (float) (Math.random() * 0.8f - 0.4f); // ±0.4
    }

    @Override
    protected void controlUpdate(float tpf) {
         if (isDead) {
            handleDeath(tpf);
            return;
        }

        if (target == null) return;

        moveTowardPlayer(tpf);
        updateGhostControl();
    }

    private void handleDeath(float tpf) {
        deathTimer += tpf;
        if (deathTimer >= 2f) {
            spatial.removeFromParent();
        }
    }

    private void moveTowardPlayer(float tpf) {
        if (target == null) return;

        Vector3f currentPos = spatial.getLocalTranslation();
        Vector3f targetPos = target.getWorldTranslation(); // Posición del jugador

        Vector3f direction = targetPos.subtract(currentPos);
        float distance = direction.length();

        // Rotación suave hacia el jugador
        if (distance > 0.1f) {
            Quaternion targetRotation = new Quaternion();
            targetRotation.lookAt(direction, Vector3f.UNIT_Y);
            spatial.getLocalRotation().slerp(targetRotation, tpf * rotationSpeed);
            spatial.setLocalRotation(spatial.getLocalRotation());
        }

        // Movimiento hacia el jugador
        float currentSpeed = speed + speedVariation;
        spatial.move(direction.normalize().mult(currentSpeed * tpf));
    }


    /*private void advanceToNextWaypoint() {
        currentWaypoint++;
        if (currentWaypoint >= path.getChildren().size()) {
            reachEndOfPath();
        }
    }*/

    private void reachEndOfPath() {
        // Aquí podrías dañar al jugador si el enemigo llega al final
        // ((Main)getApplication()).damagePlayer(10);
        spatial.removeFromParent();
    }

    private void updateGhostControl() {
        GhostControl ghost = spatial.getControl(GhostControl.class);
        if (ghost != null) {
            ghost.setPhysicsLocation(spatial.getWorldTranslation());
        }
    }
    
    private Vector3f velocity = new Vector3f(); // Velocidad acumulada
    

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        // No necesario para esta implementación
    }

    public void die() {
        if (!isDead) {
            isDead = true;
            // Rotación de caída más realista
            spatial.rotate((float)Math.toRadians(90), 0, 0);
            // Desactivar colisiones
            GhostControl ghost = spatial.getControl(GhostControl.class);
            if (ghost != null) {
                ghost.setEnabled(false);
            }
        }
    }

    public boolean isDead() {
        return isDead;
    }

    public void markDead() {
        this.isDead = true;
    }
}