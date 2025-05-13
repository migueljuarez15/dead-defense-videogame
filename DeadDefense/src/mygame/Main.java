package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.*;
import com.jme3.light.*;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.scene.shape.*;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;

public class Main extends SimpleApplication implements PhysicsCollisionListener {

    private Node enemyPath;
    private float spawnTimer = 0;
    private float spawnInterval = 5f;
    private BulletAppState bulletAppState;
    private AudioNode hitSound;
    private int playerHealth = 100;
    private int bulletsFired = 0;
    private BitmapText healthText, bulletsText;

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // 🔴 Mueve esto arriba de todo
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        bulletAppState.getPhysicsSpace().addCollisionListener(this);

        // ✅ Ahora puedes usar bulletAppState sin error
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.5f, -1f, -0.5f).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);

        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(0.3f));
        rootNode.addLight(ambient);

        createGround();
        createPath();
        createPlayer();

        flyCam.setEnabled(true);
        flyCam.setMoveSpeed(10f);
        inputManager.setCursorVisible(false);

        inputManager.addMapping("Shoot", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(actionListener, "Shoot");

        hitSound = new AudioNode(assetManager, "Sounds/Disparo.wav", AudioData.DataType.Buffer);
        hitSound.setLooping(false);
        hitSound.setPositional(false);
        hitSound.setVolume(5f);
        rootNode.attachChild(hitSound);
        
        // Fuente del HUD
        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");

        // Mira centrada
        BitmapText crosshair = new BitmapText(font, false);
        crosshair.setSize(font.getCharSet().getRenderedSize() * 2);
        crosshair.setText("+");
        crosshair.setLocalTranslation(
            settings.getWidth() / 2 - crosshair.getLineWidth() / 2,
            settings.getHeight() / 2 + crosshair.getLineHeight() / 2,
            0);
        guiNode.attachChild(crosshair);

        // Texto de vida
        BitmapText healthText = new BitmapText(font, false);
        healthText.setSize(font.getCharSet().getRenderedSize());
        healthText.setLocalTranslation(10, settings.getHeight() - 10, 0);
        healthText.setText("Vida: 100");
        guiNode.attachChild(healthText);

        // HUD: Contador de balas
        bulletsText = new BitmapText(font, false);
        bulletsText.setSize(font.getCharSet().getRenderedSize());
        bulletsText.setColor(ColorRGBA.White);
        bulletsText.setLocalTranslation(10, settings.getHeight() - 30, 0); // Posición en pantalla
        guiNode.attachChild(bulletsText);
        updateBulletText(); // Para mostrar desde el inicio

        // Texto de munición infinita
        BitmapText ammoText = new BitmapText(font, false);
        ammoText.setSize(font.getCharSet().getRenderedSize());
        ammoText.setLocalTranslation(10, settings.getHeight() - 50, 0);
        ammoText.setText("Munición: Infinita");
        guiNode.attachChild(ammoText);
    }


    @Override
    public void simpleUpdate(float tpf) {
        spawnTimer += tpf;
        if (spawnTimer > spawnInterval) {
            spawnTimer = 0;
            spawnEnemy();
        }
    }

    private void createGround() {
        Box box = new Box(30, 0.1f, 30);
        Geometry ground = new Geometry("Ground", box);
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", ColorRGBA.DarkGray);
        mat.setColor("Ambient", ColorRGBA.DarkGray);
        ground.setMaterial(mat);
        ground.setLocalTranslation(0, -0.1f, 0);
        rootNode.attachChild(ground);

        // Añadir físicas al suelo
        RigidBodyControl groundControl = new RigidBodyControl(0.0f);
        ground.addControl(groundControl);
        bulletAppState.getPhysicsSpace().add(groundControl);
    }

    private void createPath() {
        enemyPath = new Node("Path");

        Vector3f[] points = {
            new Vector3f(-10, 0, -10),
            new Vector3f(-5, 0, 0),
            new Vector3f(0, 0, 5),
            new Vector3f(5, 0, 10),
            new Vector3f(10, 0, 15),
        };

        for (Vector3f point : points) {
            Geometry marker = createMarker(point);
            enemyPath.attachChild(marker);
        }

        rootNode.attachChild(enemyPath);
    }

    private Geometry createMarker(Vector3f location) {
        Box box = new Box(0.5f, 0.1f, 0.5f);
        Geometry marker = new Geometry("Point", box);
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", ColorRGBA.Red);
        mat.setColor("Ambient", ColorRGBA.Red);
        marker.setMaterial(mat);
        marker.setLocalTranslation(location);
        return marker;
    }

    private void spawnEnemy() {
        // Cargar el modelo del enemigo (debe estar convertido a .j3o)
        Spatial enemy = assetManager.loadModel("Models/Enemigos/skeleton.j3o");
        enemy.setName("Enemy");
        enemy.setLocalTranslation(enemyPath.getChild(0).getLocalTranslation().clone());

        // ✅ Añadir hitbox que se mueve con el enemigo
        CapsuleCollisionShape shape = new CapsuleCollisionShape(0.5f, 1f);
        GhostControl ghostControl = new GhostControl(shape);
        enemy.addControl(ghostControl);
        bulletAppState.getPhysicsSpace().add(ghostControl);

        // ✅ Añadir comportamiento
        EnemigoControl control = new EnemigoControl(enemyPath);
        enemy.addControl(control);

        rootNode.attachChild(enemy);
    }


    private void createPlayer() {
        // Cubo como jugador
        Box box = new Box(0.5f, 1f, 0.5f);
        Geometry player = new Geometry("Player", box);
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", ColorRGBA.Blue);
        mat.setColor("Ambient", ColorRGBA.Blue);
        player.setMaterial(mat);
        player.setLocalTranslation(0, 1, 0);

        JugadorControl control = new JugadorControl(cam);
        player.addControl(control);
        rootNode.attachChild(player);

        inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Backward", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addListener(control, "Forward", "Backward", "Left", "Right");
    }

    private void shoot() {
        Sphere sphere = new Sphere(8, 8, 0.2f);
        Geometry bullet = new Geometry("Bullet", sphere);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Red);
        bullet.setMaterial(mat);

        Vector3f camDir = cam.getDirection().normalize();
        Vector3f camLoc = cam.getLocation().add(camDir.mult(1f));
        bullet.setLocalTranslation(camLoc);

        rootNode.attachChild(bullet);

        RigidBodyControl bulletControl = new RigidBodyControl(1f);
        bullet.addControl(bulletControl);
        bulletControl.setLinearVelocity(camDir.mult(20));

        bulletAppState.getPhysicsSpace().add(bulletControl);

        // ✅ Añade el control para eliminarla si no impacta
        bullet.addControl(new BalaControl(bulletAppState));
        
        bulletsFired++;
        bulletsText.setText("Balas disparadas: " + bulletsFired);
    }


    private ActionListener actionListener = new ActionListener() {
        public void onAction(String name, boolean isPressed, float tpf) {
            if (name.equals("Shoot") && isPressed) {
                shoot();
            }
        }
    };
    
    private PhysicsSpace getPhysicsSpace() {
        return bulletAppState.getPhysicsSpace();
    }

    public void collision(PhysicsCollisionEvent event) {
        Spatial a = event.getNodeA();
        Spatial b = event.getNodeB();
        
        if (a == null || b == null) return;

        Spatial bala = null;
        Spatial enemigo = null;
        Spatial jugador = null;

        if ("Bullet".equals(a.getName()) && "Enemy".equals(b.getName())) {
            bala = a;
            enemigo = b;
        } else if ("Enemy".equals(a.getName()) && "Bullet".equals(b.getName())) {
            bala = b;
            enemigo = a;
        }

        if (bala != null && enemigo != null) {
            EnemigoControl ec = enemigo.getControl(EnemigoControl.class);
            if (ec != null && !ec.isDead()) {
                ec.markDead();
                hitSound.playInstance();

                enemigo.rotate(-FastMath.HALF_PI, 0, 0);

                enemigo.addControl(new RemoverDespuesControl(2f, bulletAppState));
            }

            getPhysicsSpace().remove(bala.getControl(RigidBodyControl.class));
            bala.removeFromParent();
        }
        
        // 🟢 Detectar jugador y enemigo
        if ("Player".equals(a.getName()) && "Enemy".equals(b.getName())) {
            jugador = a;
            enemigo = b;
        } else if ("Enemy".equals(a.getName()) && "Player".equals(b.getName())) {
            jugador = b;
            enemigo = a;
        }

        if (jugador != null && enemigo != null) {
            damagePlayer(10); // Puedes ajustar el valor del daño
            // Opcionalmente, evitar múltiples daños por el mismo enemigo:
            // enemigo.setName("EnemyHit");
        }
    }
    
    private void updateBulletText() {
        if (bulletsText != null) {
            bulletsText.setText("Balas disparadas: " + bulletsFired);
        }
    }

    private void enemyHit(Spatial enemy) {
        EnemigoControl control = enemy.getControl(EnemigoControl.class);
        if (control != null) {
            control.die();
        }

        if (hitSound != null) {
            hitSound.playInstance();
        }
    }
    
    public void damagePlayer(int amount) {
        playerHealth -= amount;
        if (playerHealth < 0) playerHealth = 0;
        healthText.setText("Vida: " + playerHealth);
    
        if (playerHealth == 0) {
            // Muestra un mensaje de muerte
            BitmapText gameOverText = new BitmapText(assetManager.loadFont("Interface/Fonts/Default.fnt"), false);
            gameOverText.setSize(40);
            gameOverText.setText("¡Has muerto!");
            gameOverText.setLocalTranslation(settings.getWidth() / 2 - 100, settings.getHeight() / 2, 0);
            guiNode.attachChild(gameOverText);
        
            // Puedes también detener el juego si quieres
            // stop();
        }
    }
}