package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.*;
import com.jme3.light.*;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;

public class Main extends SimpleApplication {

    private Node enemyPath;
    private float spawnTimer = 0;
    private float spawnInterval = 5f;
    private BulletAppState bulletAppState;

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // ✅ Luz direccional
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.5f, -1f, -0.5f).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);

        // ✅ Luz ambiental
        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(0.3f));
        rootNode.addLight(ambient);

        // ✅ Terreno/piso visible
        createGround();

        // ✅ Crear camino de enemigos
        createPath();

        // ✅ Crear jugador
        createPlayer();

        // ✅ Posicionar cámara desde arriba
        /*cam.setLocation(new Vector3f(0, 20, 20));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

        // Desactivar cámara libre
        flyCam.setEnabled(false);*/
        
        flyCam.setEnabled(true);
        flyCam.setMoveSpeed(10f);
        inputManager.setCursorVisible(false);
        
        inputManager.addMapping("Shoot", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(actionListener, "Shoot");
        
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
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
        Box box = new Box(0.5f, 0.5f, 0.5f);
        Geometry enemy = new Geometry("Enemy", box);
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", ColorRGBA.Green);
        mat.setColor("Ambient", ColorRGBA.Green);
        enemy.setMaterial(mat);
        enemy.setLocalTranslation(enemyPath.getChild(0).getLocalTranslation().clone());

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

        // Posición inicial: frente a la cámara
        Vector3f camDir = cam.getDirection().normalize();
        Vector3f camLoc = cam.getLocation().add(camDir.mult(1f)); // justo frente a la cámara
        bullet.setLocalTranslation(camLoc);

        // Añadir al mundo
        rootNode.attachChild(bullet);

        // Usamos RigidBodyControl para que se mueva
        RigidBodyControl bulletControl = new RigidBodyControl(1f);
        bullet.addControl(bulletControl);
        bulletControl.setLinearVelocity(camDir.mult(20)); // velocidad de disparo

        bulletAppState.getPhysicsSpace().add(bulletControl);
    }
    
    private ActionListener actionListener = new ActionListener() {
        public void onAction(String name, boolean isPressed, float tpf) {
            if (name.equals("Shoot") && isPressed) {
                shoot();
            }
        }
    };
}