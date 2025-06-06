package mygame;

import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.bullet.control.GhostControl;
import com.jme3.math.Quaternion;
import com.jme3.math.FastMath; 

/**
 * Dead Defense: El videojuego
 * @author Miguel Ángel Juárez Martínez
 * @author Franco Sánchez Gutierrez
 * @author Braulio Adrián Bollaín y Goytia Ortega
 */
public class EnemigoControl extends AbstractControl {
    // Variables mejoradas
    private int currentWaypoint = 0;  // Punto actual en el camino
    private float speed = 3.5f;       // Velocidad base
    private boolean isDead = false;   // Estado del enemigo
    private float deathTimer = 0f;    // Temporizador para eliminación
    private float rotationSpeed = 4f; // Velocidad de rotación suavizada
    private Spatial target; // El jugador
    // Nueva variable para variación de velocidad
    private float speedVariation = 0f;
    private float speedBase = 3.5f;       // Velocidad base inicial
    private float speedMax = 7.0f;        // Velocidad máxima posible
    private float currentSpeed;           // Velocidad actual
    private float damageCooldown = 0f;
    private final float DAMAGE_COOLDOWN_TIME = 1f; // 1 segundo entre daños
    private final Main mainGame; // Referencia al juego principal
    private Vector3f velocity = new Vector3f(); // Velocidad acumulada
    
    // Modificar el constructor para recibir la instancia de Main
    public EnemigoControl(Spatial target, Main mainGame) {
        this.target = target;
        this.mainGame = mainGame;
        this.speedVariation = (float) (Math.random() * 0.8f - 0.4f);
        calcularVelocidadProgresiva();
    }
    
    private void calcularVelocidadProgresiva() {
        if (mainGame == null) {
            this.currentSpeed = speedBase + speedVariation;
            return;
        }
        
        // Calcular progresión de manera segura
        float progresion = Math.min(1f, Math.max(0f, 
            (float)mainGame.puntos / mainGame.puntosParaGanar));
        
        this.currentSpeed = speedBase + (speedMax - speedBase) * progresion;
        this.currentSpeed += speedVariation;
    }
    
    private void handleDeath(float tpf) {
        deathTimer += tpf;
        if (deathTimer >= 2f) {
            // Notificar al juego principal antes de eliminar
            if (mainGame != null) {
                mainGame.removerEnemigo(spatial);
            }
            spatial.removeFromParent();
        }
    }
    
    @Override
    protected void controlUpdate(float tpf) {
        if (isDead) {
            handleDeath(tpf); // Ahora este método existe
            return;
        }

        if (damageCooldown > 0) {
            damageCooldown -= tpf;
        }

        if (target == null) return;

        moveTowardPlayer(tpf);
        updateGhostControl();
    }

    // Añade este método para verificar si puede hacer daño
    public boolean canDamage() {
        return damageCooldown <= 0;
    }

    // Y este método para resetear el cooldown después de hacer daño
    public void resetDamageCooldown() {
        damageCooldown = DAMAGE_COOLDOWN_TIME;
    }

    private void moveTowardPlayer(float tpf) {
        if (target == null) return;

        Vector3f currentPos = spatial.getLocalTranslation();
        Vector3f targetPos = target.getWorldTranslation();
        Vector3f direction = targetPos.subtract(currentPos);
        float distance = direction.length();

        // Rotación suave hacia el jugador
        if (distance > 0.1f) {
            Quaternion targetRotation = new Quaternion();
            targetRotation.lookAt(direction, Vector3f.UNIT_Y);
            spatial.getLocalRotation().slerp(targetRotation, tpf * rotationSpeed);
            spatial.setLocalRotation(spatial.getLocalRotation());
        }
        
        // Movimiento hacia el jugador (solo una llamada a move)
        spatial.move(direction.normalize().mult(currentSpeed * tpf));
    }

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