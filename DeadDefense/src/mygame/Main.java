package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioData.DataType;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.*;
import com.jme3.light.*;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;

public class Main extends SimpleApplication implements PhysicsCollisionListener {

    private Node enemyPath;
    private float spawnTimer = 0;
    private float spawnInterval = 5f;
    private BulletAppState bulletAppState;
    private AudioNode hitSound;

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

        // Añadir control personalizado de movimiento
        EnemigoControl control = new EnemigoControl(enemyPath);
        enemy.addControl(control);

        // Añadir colisión fantasma para detección
        RigidBodyControl physics = new RigidBodyControl(0f); // sin masa, no afectado por física
        enemy.addControl(physics);
        bulletAppState.getPhysicsSpace().add(physics);

        // Añadir al mundo
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
        Geometry bullet = new Geometry("Bullet", sphere);  // Nombre clave para colisión
        bullet.setName("Bullet");

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
    }

    private ActionListener actionListener = new ActionListener() {
        public void onAction(String name, boolean isPressed, float tpf) {
            if (name.equals("Shoot") && isPressed) {
                shoot();
            }
        }
    };

    @Override
    public void collision(PhysicsCollisionEvent event) {
        Spatial a = event.getNodeA();
        Spatial b = event.getNodeB();

        if (a == null || b == null) return;

        if (a.getName().equals("Bullet") && b.getName().equals("Enemy")) {
            enemyHit(b);
            a.removeFromParent();
        } else if (b.getName().equals("Bullet") && a.getName().equals("Enemy")) {
            enemyHit(a);
            b.removeFromParent();
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
}