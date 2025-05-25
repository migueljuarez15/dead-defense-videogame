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
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;

public class Main extends SimpleApplication implements PhysicsCollisionListener {

    private Node[] enemyPaths;
    private int pathCount = 3;
    private float spawnTimer = 0;
    private float spawnInterval = 5f;
    private BulletAppState bulletAppState;
    private AudioNode hitSound;
    private int playerHealth = 100;
    private int bulletsFired = 0;
    private BitmapText healthText, bulletsText;
    

    public static void main(String[] args) {
        Main app = new Main();
        // Crear configuración personalizada
        AppSettings settings = new AppSettings(false);
        settings.setTitle("Dead Defense");

        // Cambiar el logo del mono
        settings.setSettingsDialogImage("/Images/deadDefense.png");

        // Asignar configuración a la app
        app.setSettings(settings);
        app.setShowSettings(true); // Muestra el diálogo al iniciar
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
        createPaths();
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
        
        //Escena para suelo de cementerio
        Box sueloBox = new Box(50, 0.15f, 50); 
        Geometry suelo = new Geometry("Suelo", sueloBox);
        Material matSuelo = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");

        // Cargar la textura
        Texture texSuelo = assetManager.loadTexture("Textures/grass.jpg");
        texSuelo.setWrap(Texture.WrapMode.Repeat);
        matSuelo.setTexture("DiffuseMap", texSuelo);

        matSuelo.setBoolean("UseMaterialColors", true);
        matSuelo.setColor("Diffuse", ColorRGBA.White);
        matSuelo.setFloat("Shininess", 2f);
        suelo.setMaterial(matSuelo);

        // Escala de las coordenadas de la textura (ajuste)
        sueloBox.scaleTextureCoordinates(new Vector2f(50, 50));  // Ajusta la escala aquí

        suelo.setLocalTranslation(0, -0.1f, 0);
        rootNode.attachChild(suelo);
        createCemeteryStructures();
        
    }
    
 
    
    private void createCemeteryStructures() {
    // Crear suelo del cementerio
    Box sueloBox = new Box(50, 0.12f, 50);
    Geometry suelo = new Geometry("Suelo", sueloBox);
    Material matSuelo = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");

    // Cargar la textura
    Texture texSuelo = assetManager.loadTexture("Textures/grass.jpg");
    texSuelo.setWrap(Texture.WrapMode.Repeat);
    matSuelo.setTexture("DiffuseMap", texSuelo);
    matSuelo.setBoolean("UseMaterialColors", true);
    matSuelo.setColor("Diffuse", ColorRGBA.White);
    matSuelo.setFloat("Shininess", 2f);
    suelo.setMaterial(matSuelo);

    // Ajustar coordenadas de la textura
    sueloBox.scaleTextureCoordinates(new Vector2f(50, 50));
    suelo.setLocalTranslation(0, -0.1f, 0);
    
    rootNode.attachChild(suelo);

    
    int tombCount = 25; // Cantidad de tumbas
    float areaSize = 80f; // Área dentro de la cual se esparcen
    java.util.Random rand = new java.util.Random();

    for (int i = 0; i < tombCount; i++) {
        float x = rand.nextFloat() * areaSize - areaSize / 2f;
        float z = rand.nextFloat() * areaSize - areaSize / 2f;
        Vector3f pos = new Vector3f(x, 0, z);
        createTomb(pos);
        }
    }
    
    
    private void createTomb(Vector3f position) {
    Node tombNode = new Node("Tomb_" + position.toString());
    
    Box tombBase = new Box(1.2f, 0.3f, 0.8f); 
    Geometry tombBaseGeometry = new Geometry("TombBase", tombBase);
    
    // Material con textura de piedra
    Material tombMaterial = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
    Texture stoneTexture = assetManager.loadTexture("Textures/piedras_lapida.jpg");
    stoneTexture.setWrap(Texture.WrapMode.Repeat);
    tombMaterial.setTexture("DiffuseMap", stoneTexture);
    tombMaterial.setBoolean("UseMaterialColors", true);
    tombMaterial.setColor("Diffuse", ColorRGBA.Gray.mult(0.8f));
    tombMaterial.setFloat("Shininess", 5f);
    tombBaseGeometry.setMaterial(tombMaterial);
    tombBaseGeometry.setLocalTranslation(0, 0, 0);
    tombBase.scaleTextureCoordinates(new Vector2f(2f, 2f));
    
    // Lápida 
    Box headstone = new Box(0.7f, 0.8f, 0.05f);
    Geometry headstoneGeometry = new Geometry("Headstone", headstone);
    Material headstoneMaterial = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
    Texture headstoneTexture = assetManager.loadTexture("Textures/lapida_frontal.jpg");
    headstoneTexture.setWrap(Texture.WrapMode.Repeat);
    headstoneMaterial.setTexture("DiffuseMap", headstoneTexture);
    headstoneMaterial.setColor("Diffuse", ColorRGBA.White.mult(0.9f));
    headstoneMaterial.setFloat("Shininess", 10f);
    headstoneGeometry.setMaterial(headstoneMaterial);
    headstoneGeometry.setLocalTranslation(0, 0.8f, -0.85f); 
    
    
    // Añadir todos los componentes al nodo
    tombNode.attachChild(tombBaseGeometry);
    tombNode.attachChild(headstoneGeometry);
    
    tombNode.setLocalTranslation(position);
    
    // Fisica
    CollisionShape tombShape = CollisionShapeFactory.createMeshShape(tombNode);
    RigidBodyControl tombPhysics = new RigidBodyControl(tombShape, 0);
    tombNode.addControl(tombPhysics);
    bulletAppState.getPhysicsSpace().add(tombPhysics);
    
    rootNode.attachChild(tombNode);
    
    
    
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

    private void createPaths() {
        enemyPaths = new Node[pathCount];

        // Definimos los puntos para cada camino
        Vector3f[][] pathsPoints = {
            // Camino 1 (original)
            {
                new Vector3f(-10, 0, -10),
                new Vector3f(-5, 0, 0),
                new Vector3f(0, 0, 5),
                new Vector3f(5, 0, 10),
                new Vector3f(10, 0, 15)
            },
            // Camino 2 (alternativo)
            {
                new Vector3f(-15, 0, 0),
                new Vector3f(-10, 0, 5),
                new Vector3f(-5, 0, 0),
                new Vector3f(0, 0, -5),
                new Vector3f(5, 0, 0)
            },
            // Camino 3 (circular)
            {
                new Vector3f(0, 0, -10),
                new Vector3f(5, 0, -5),
                new Vector3f(10, 0, 0),
                new Vector3f(5, 0, 5),
                new Vector3f(0, 0, 10)
            }
        };

        // Creamos cada camino
        for (int i = 0; i < pathCount; i++) {
            enemyPaths[i] = new Node("Path_" + i);

            for (Vector3f point : pathsPoints[i]) {
                Geometry marker = createMarker(point);
                // Asignamos diferente color a cada camino
                Material mat = marker.getMaterial();
                mat.setColor("Diffuse", ColorRGBA.Blue.mult(i+1));
                enemyPaths[i].attachChild(marker);
            }

            rootNode.attachChild(enemyPaths[i]);
        }
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
        // Elegir camino aleatorio (0, 1 o 2)
        int pathIndex = FastMath.rand.nextInt(pathCount);
        Node selectedPath = enemyPaths[pathIndex];
        
        // Cargar el modelo del enemigo (debe estar convertido a .j3o)
        Spatial enemy = assetManager.loadModel("Models/Enemigos/skeleton.j3o");
        enemy.setName("Enemy");
        enemy.setLocalTranslation(selectedPath.getChild(0).getLocalTranslation().clone()); //Eleccion del camino aleatorio

        // ✅ Añadir hitbox que se mueve con el enemigo
        CapsuleCollisionShape shape = new CapsuleCollisionShape(0.5f, 1f);
        GhostControl ghostControl = new GhostControl(shape);
        enemy.addControl(ghostControl);
        bulletAppState.getPhysicsSpace().add(ghostControl);

        // ✅ Añadir comportamiento
        EnemigoControl control = new EnemigoControl(selectedPath);
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