package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioData.DataType;
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
import com.jme3.util.SkyFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.jme3.scene.control.AbstractControl; 
import com.jme3.renderer.RenderManager;       
import com.jme3.renderer.ViewPort;        
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.effect.shapes.EmitterSphereShape;

public class Main extends SimpleApplication implements PhysicsCollisionListener {

    
   
    private Spatial player;
    private Node[] enemyPaths;
    private int pathCount = 3;
    private float spawnTimer = 0;
    private float spawnInterval = 5f;
    private BulletAppState bulletAppState;
    private AudioNode hitSound, ambientSound, towerHit;
    private int playerHealth = 100;
    private int bulletsFired = 0;
    private BitmapText healthText, bulletsText, towerHealthT;
    private Node torre;
    private List<Spatial> enemys = new ArrayList<>();
    
    public int puntos = 0;
    protected int puntosParaGanar = 300; // Meta para ganar el juego
    private float spawnIntervalBase = 4f; 
    private float spawnIntervalMin = 0.5f; 
    private BitmapText puntosText; 
    private int enemigosPorOleada = 1;
    private int maxEnemigosEnEscena = 5; // Máximo de enemigos permitidos en pantalla
    protected int enemigosEnEscena = 0;     
    private int enemigosPorOleadaBase = 2; 
    private int enemigosPorOleadaMax = 5;  
    private boolean juegoGanado = false;
    private boolean juegoPerdido = false;
    
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
    
    private ParticleEmitter rainEmitter;
    
    @Override
    public void simpleInitApp() {
        // 🔴 Mueve esto arriba de todo
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        bulletAppState.getPhysicsSpace().addCollisionListener(this);
        
        // En simpleInitApp(), después de crear bulletAppState:
        bulletAppState.getPhysicsSpace().setMaxSubSteps(2); 
        bulletAppState.getPhysicsSpace().setAccuracy(0.016f); 

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
        inputManager.addListener(actionListenerShoot, "Shoot");

        hitSound = new AudioNode(assetManager, "Sounds/hitEnemy.wav", AudioData.DataType.Buffer);
        hitSound.setLooping(false);
        hitSound.setPositional(false);
        hitSound.setVolume(5f);
        rootNode.attachChild(hitSound);
        
        towerHit = new AudioNode(assetManager, "Sounds/hitTower.wav", AudioData.DataType.Buffer);
        towerHit.setLooping(false);
        towerHit.setPositional(false);
        towerHit.setVolume(3.8f);
        rootNode.attachChild(towerHit);
        
        initAmbientSound();
       
        Spatial sky = SkyFactory.createSky(
        assetManager,
        "Textures/Cielo/render.dds",
        SkyFactory.EnvMapType.CubeMap
        );
        rootNode.attachChild(sky);

        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        puntosText = new BitmapText(font, false);
        puntosText.setSize(font.getCharSet().getRenderedSize());
        puntosText.setLocalTranslation(10, settings.getHeight() - 90, 0);
        puntosText.setText("Puntos: 0");
        guiNode.attachChild(puntosText);    

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
        healthText = new BitmapText(font, false);
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
        
        // Cargar la torre
        torre = (Node) assetManager.loadModel("Models/Torre/tower.j3o");
        torre.setLocalScale(5f);
        torre.setLocalTranslation(new Vector3f(0, 0, 26));
        CollisionShape torreShape = CollisionShapeFactory.createMeshShape(torre);
        RigidBodyControl torreFisica = new RigidBodyControl(torreShape, 0); // masa 0 = estático
        torre.addControl(new TorreControl());
        torre.addControl(torreFisica);
        rootNode.attachChild(torre);
        bulletAppState.getPhysicsSpace().add(torreFisica);

        // HUD para la vida de la torre
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        towerHealthT = new BitmapText(font, false);
        towerHealthT.setSize(guiFont.getCharSet().getRenderedSize());
        towerHealthT.setLocalTranslation(10, settings.getHeight() - 70, 0);
        guiNode.attachChild(towerHealthT);
        
         crearEfectoLluvia();
   
        // Mapeo de teclas para reiniciar
        inputManager.addMapping("RestartGame", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addListener(actionListenerRestart, "RestartGame");
    }
    

    public void addPuntos(int cantidad) {
        puntos += cantidad;
        puntosText.setText("Puntos: " + puntos);
    
        if (puntos >= puntosParaGanar) {
            mostrarMensajeVictoria();
        }
    
        ajustarDificultad();
    }
    
    private void mostrarMensajeMuerte() {
    juegoPerdido = true;
    
    // Mostrar mensaje de muerte
    BitmapText gameOverText = new BitmapText(guiFont, false);
    gameOverText.setSize(40);
    gameOverText.setText("¡Has muerto!");
    gameOverText.setColor(ColorRGBA.Red);
    gameOverText.setLocalTranslation(
        settings.getWidth() / 2 - gameOverText.getLineWidth() / 2,
        settings.getHeight() / 2 + 50,
        0);
    guiNode.attachChild(gameOverText);
    
    // Mostrar mensaje de reinicio
    BitmapText restartText = new BitmapText(guiFont, false);
    restartText.setSize(24);
    restartText.setText("Presiona 'R' para reiniciar");
    restartText.setColor(ColorRGBA.White);
    restartText.setLocalTranslation(
        settings.getWidth() / 2 - restartText.getLineWidth() / 2,
        settings.getHeight() / 2,
        0);
    guiNode.attachChild(restartText);
    
    // Desactivar controles
    inputManager.deleteMapping("Forward");
    inputManager.deleteMapping("Backward");
    inputManager.deleteMapping("Left");
    inputManager.deleteMapping("Right");
    inputManager.deleteMapping("Shoot");
    
    // Detener sonidos
    ambientSound.stop();
}
    
    private void mostrarMensajeVictoria() {
        juegoGanado = true;
    
        // Eliminar todos los enemigos existentes
        Iterator<Spatial> iterator = enemys.iterator();
        while (iterator.hasNext()) {
            Spatial enemigo = iterator.next();
            rootNode.detachChild(enemigo);
            iterator.remove();
            enemigosEnEscena--;
        }
    
        // Mostrar mensaje de Victoria
        BitmapText victoriaText = new BitmapText(guiFont, false);
        victoriaText.setSize(60);
        victoriaText.setText("¡VICTORIA!");
        victoriaText.setColor(ColorRGBA.Green);
        victoriaText.setLocalTranslation(
            settings.getWidth() / 2 - victoriaText.getLineWidth() / 2,
            settings.getHeight() / 2,
            0);
        guiNode.attachChild(victoriaText);
        
        // Mostrar mensaje de Reinicio
        BitmapText restartText = new BitmapText(guiFont, false);
        restartText.setSize(20);
        restartText.setText("¡Presiona 'R' para reiniciar el juego!");
        restartText.setColor(ColorRGBA.White);
        restartText.setLocalTranslation(
            settings.getWidth() - restartText.getLineWidth(),
            settings.getHeight(),
            0);
        guiNode.attachChild(restartText);
    
        // Detener sonido ambiente
        ambientSound.stop();
        
        // No poder moverse
        inputManager.deleteMapping("Forward");
        inputManager.deleteMapping("Backward");
        inputManager.deleteMapping("Left");
        inputManager.deleteMapping("Right");
    }

    private void ajustarDificultad() {
        float progresion = (float) puntos / puntosParaGanar;
    
        spawnInterval = spawnIntervalBase - (spawnIntervalBase - spawnIntervalMin) * (progresion * 1.2f);
        spawnInterval = Math.max(spawnInterval, spawnIntervalMin);
    
        // Aumento más rápido de enemigos por oleada
        enemigosPorOleada = enemigosPorOleadaBase + (int)(progresion * (enemigosPorOleadaMax - enemigosPorOleadaBase));
    
        // Asegurarse de que no exceda el máximo
        enemigosPorOleada = Math.min(enemigosPorOleada, enemigosPorOleadaMax);
    }
    
    
    
    private void crearEfectoLluvia() {
        rainEmitter = new ParticleEmitter("Rain", ParticleMesh.Type.Triangle, 2500);
        Material rainMat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        rainMat.setTexture("Texture", assetManager.loadTexture("Textures/rain3.png"));
        rainEmitter.setMaterial(rainMat);

        rainEmitter.setShape(new EmitterSphereShape(Vector3f.ZERO, 50f));
        rainEmitter.setLocalTranslation(0, 30, 0); 

        rainEmitter.setStartSize(0.1f);
        rainEmitter.setEndSize(0.1f);
        rainEmitter.setStartColor(ColorRGBA.White);
        rainEmitter.setEndColor(new ColorRGBA(0.8f, 0.8f, 1f, 0.5f));

        rainEmitter.setGravity(0, -10, 0);
        rainEmitter.setLowLife(1.5f);
        rainEmitter.setHighLife(2f);
        rainEmitter.setParticlesPerSec(1000);
        rainEmitter.getParticleInfluencer().setInitialVelocity(new Vector3f(0, -15, 0));
        rainEmitter.getParticleInfluencer().setVelocityVariation(0.3f);

        rootNode.attachChild(rainEmitter);
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

        // Posiciones definidas para las tumbas
        Vector3f[] tombPositions = {
            new Vector3f(-20, 0, 5),
            new Vector3f(-22, 0, -9),
            new Vector3f(-13, 0, 9),
            new Vector3f(-7, 0, -10),
            new Vector3f(20, 0, -15),
            new Vector3f(12, 0, -4),
            new Vector3f(18, 0, 12),
            new Vector3f(-5, 0, 16),
            new Vector3f(-3, 0, 11),
        };

        for (Vector3f pos : tombPositions) {
            crearTumba(pos);
        }
    
        Vector3f[] cruzPosicion = {
            new Vector3f(-25, 0, 8),
            new Vector3f(-10, 0, -22),
            new Vector3f(-7, 0, 12),
            new Vector3f(-1, 0, -20),
            new Vector3f(30, 0, -8),
            new Vector3f(23, 0, 15),
            new Vector3f(33, 0, 17),
            new Vector3f(-23, 0, 4),
            new Vector3f(10, 0, 25),
        };

        for (Vector3f pos : cruzPosicion) {
            crearCruz(pos);
        }
    }
    
    private void crearCruz(Vector3f posicion) {
        // Barra vertical de la cruz
        Box barraVertical = new Box(0.2f, 3f, 0.2f); 
        Geometry barraVerticalGeo = new Geometry("Barra Vertical", barraVertical);
        Material matCruz = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        matCruz.setColor("Diffuse", ColorRGBA.Gray);  // Color gris para la cruz
        barraVerticalGeo.setMaterial(matCruz);
        barraVerticalGeo.setLocalTranslation(posicion.add(new Vector3f(0, 1.5f, 0)));  
        rootNode.attachChild(barraVerticalGeo);

        // Barra horizontal de la cruz
        Box barraHorizontal = new Box(1.2f, 0.2f, 0.2f); 
        Geometry barraHorizontalGeo = new Geometry("Barra Horizontal", barraHorizontal);
        barraHorizontalGeo.setMaterial(matCruz);
        barraHorizontalGeo.setLocalTranslation(posicion.add(new Vector3f(0, 3, 0)));  
    
        rootNode.attachChild(barraHorizontalGeo);
    }

    private void crearTumba(Vector3f position) {
        Node tumbaNodo = new Node("Tomb_" + position.toString());
    
        Box tombBase = new Box(1.2f, 0.3f, 0.8f); 
        Geometry baseTumbaGeometria = new Geometry("TombBase", tombBase);
    
        // Material con textura de piedra
        Material tumbaMaterial = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        Texture stoneTexture = assetManager.loadTexture("Textures/piedras_lapida.jpg");
        stoneTexture.setWrap(Texture.WrapMode.Repeat);
        tumbaMaterial.setTexture("DiffuseMap", stoneTexture);
        tumbaMaterial.setBoolean("UseMaterialColors", true);
        tumbaMaterial.setColor("Diffuse", ColorRGBA.Gray.mult(0.8f));
        tumbaMaterial.setFloat("Shininess", 5f);
        baseTumbaGeometria.setMaterial(tumbaMaterial);
        baseTumbaGeometria.setLocalTranslation(0, 0, 0);
        tombBase.scaleTextureCoordinates(new Vector2f(2f, 2f));
    
        // Tumbas 
        Box headstone = new Box(0.7f, 0.8f, 0.05f);
        Geometry tumbaGeometria = new Geometry("Headstone", headstone);
        Material tumbaMaterialT = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        Texture tumbaTextura = assetManager.loadTexture("Textures/lapida_frontal.jpg");
        tumbaTextura.setWrap(Texture.WrapMode.Repeat);
        tumbaMaterial.setTexture("DiffuseMap", tumbaTextura);
        tumbaMaterial.setColor("Diffuse", ColorRGBA.White.mult(0.9f));
        tumbaMaterial.setFloat("Shininess", 10f);
        tumbaGeometria.setMaterial(tumbaMaterial);
        tumbaGeometria.setLocalTranslation(0, 0.8f, -0.85f); 
    
        // Añadir todos los componentes al nodo
        tumbaNodo.attachChild(baseTumbaGeometria);
        tumbaNodo.attachChild(tumbaGeometria);
    
        tumbaNodo.setLocalTranslation(position);
    
        // Fisica tumba
        CollisionShape tumbaForma = CollisionShapeFactory.createMeshShape(tumbaNodo);
        RigidBodyControl tumbaFisica = new RigidBodyControl(tumbaForma, 0);
        tumbaNodo.addControl(tumbaFisica);
        bulletAppState.getPhysicsSpace().add(tumbaFisica);
    
        rootNode.attachChild(tumbaNodo);
    }

    // Método para manejar la derrota
    private void mostrarMensajeDerrota() {
        juegoPerdido = true;

        // Eliminar todos los enemigos existentes
        Iterator<Spatial> iterator = enemys.iterator();
        while (iterator.hasNext()) {
            Spatial enemigo = iterator.next();
            rootNode.detachChild(enemigo);
            iterator.remove();
            enemigosEnEscena--;
        }

        // Mostrar mensaje de derrota
        BitmapText derrotaText = new BitmapText(guiFont, false);
        derrotaText.setSize(60);
        derrotaText.setText("¡DERROTA!");
        derrotaText.setColor(ColorRGBA.Red);
        derrotaText.setLocalTranslation(
            settings.getWidth() / 2 - derrotaText.getLineWidth() / 2,
            settings.getHeight() / 2,
            0);
        guiNode.attachChild(derrotaText);

        // Mostrar mensaje de Reinicio
        BitmapText restartText = new BitmapText(guiFont, false);
        restartText.setSize(20);
        restartText.setText("¡Presiona 'R' para reiniciar el juego!");
        restartText.setColor(ColorRGBA.White);
        restartText.setLocalTranslation(
            settings.getWidth() - restartText.getLineWidth(),
            settings.getHeight(),
            0);
        guiNode.attachChild(restartText);
    
        // Detener sonido ambiente
        ambientSound.stop();
        
        // No poder moverse
        inputManager.deleteMapping("Forward");
        inputManager.deleteMapping("Backward");
        inputManager.deleteMapping("Left");
        inputManager.deleteMapping("Right");
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (juegoGanado || juegoPerdido) return;

        // Verificar estado de la torre
            TorreControl tc = torre.getControl(TorreControl.class);
            if (tc != null && tc.getVida() <= 0 && !juegoPerdido) {
                tc.destruirTorre();
                mostrarMensajeDerrota();
                return;
            }

        spawnTimer += tpf;
        if (spawnTimer > spawnInterval) {
            spawnTimer = 0;
            spawnEnemy();
        }
    
        // Actualizar texto HUD con la vida de la torre
        if (tc != null) {
            towerHealthT.setText("Vida Torre: " + tc.getVida());
        }

        // Detectar colisión entre enemigos y la torre
        Iterator<Spatial> iterator = enemys.iterator();
        while (iterator.hasNext()) {
            Spatial enemigo = iterator.next();
            if (enemigo.getWorldTranslation().distance(torre.getWorldTranslation()) < 2f) {
                if (tc != null) {
                    tc.recibirDanno(10);
                    towerHit.playInstance();
                }
                rootNode.detachChild(enemigo);
                iterator.remove();
                enemigosEnEscena--;
            }
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
                
                enemyPaths[i].attachChild(marker);
            }

            rootNode.attachChild(enemyPaths[i]);
        }
    }

    private Geometry createMarker(Vector3f location) {
    Sphere rocaForma = new Sphere(16, 16, 0.3f); // Radio de 0.3
    Geometry roca = new Geometry("RockMarker", rocaForma);
    
    Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
    // Cargar textura de roca
    Texture rocaTx = assetManager.loadTexture("Textures/piedra-negro.jpg");
    mat.setTexture("DiffuseMap", rocaTx);
    mat.setBoolean("UseMaterialColors", true);
    mat.setColor("Diffuse", ColorRGBA.Gray); // Color base gris
    mat.setFloat("Shininess", 5f); // Brillo moderado
    
    roca.setMaterial(mat);
    roca.setLocalTranslation(location);
    return roca;
}

    public synchronized void removerEnemigo(Spatial enemigo) {
        if (enemys.remove(enemigo)) {
            enemigosEnEscena--;
        }
    }
    
    private void spawnEnemy() {
        if (juegoGanado || juegoPerdido) return;
        
        // Verificación más estricta del límite
        synchronized(this) {
            if (enemigosEnEscena >= maxEnemigosEnEscena) {
                return;
            }
            
            int disponibles = maxEnemigosEnEscena - enemigosEnEscena;
            int aSpawnear = Math.min(enemigosPorOleada, disponibles);
            
            for(int i = 0; i < aSpawnear; i++) {
                if (enemigosEnEscena >= maxEnemigosEnEscena) break;
                
                int pathIndex = FastMath.rand.nextInt(pathCount);
                Node selectedPath = enemyPaths[pathIndex];

                Spatial enemy = assetManager.loadModel("Models/Enemigo/skeleton.j3o");
                enemy.setLocalScale(1.5f);
                enemy.setName("Enemy");
                enemy.setLocalTranslation(selectedPath.getChild(0).getLocalTranslation().clone());

                CapsuleCollisionShape shape = new CapsuleCollisionShape(1.3f, 1.8f);
                GhostControl ghostControl = new GhostControl(shape);
                enemy.addControl(ghostControl);
                bulletAppState.getPhysicsSpace().add(ghostControl);

                Spatial target = FastMath.nextRandomFloat() < 0.5f ? player : torre;
                
                EnemigoControl control = new EnemigoControl(target, this);
                enemy.addControl(control);

                enemy.addControl(new AbstractControl() {
                    @Override
                    protected void controlUpdate(float tpf) {}
                    
                    @Override
                    protected void controlRender(RenderManager rm, ViewPort vp) {}
                    
                    @Override
                    public void setSpatial(Spatial spatial) {
                        super.setSpatial(spatial);
                        if (spatial != null) {
                            synchronized(Main.this) {
                                enemigosEnEscena++;
                                enemys.add(spatial);
                            }
                        }
                    }
                });

                rootNode.attachChild(enemy);
            }
        }
    }
    

    

    private void createPlayer() {
        // Cargar el modelo
        player = assetManager.loadModel("Models/Jugador/playerSoldier.j3o");
        player.rotate(0, FastMath.PI, 0);
        player.setLocalScale(0.7f);
        player.setLocalTranslation(0, 1, 0);

        // Crear la forma de colisión para el jugador
        CapsuleCollisionShape capsule = new CapsuleCollisionShape(0.9f, 1.8f);
        RigidBodyControl playerPhysics = new RigidBodyControl(capsule, 100f); // masa del jugador

        player.addControl(playerPhysics);

        JugadorControl control = new JugadorControl(cam, playerPhysics);
        player.addControl(control);

        rootNode.attachChild(player);
        bulletAppState.getPhysicsSpace().add(playerPhysics); // Agrega el cuerpo físico al mundo

        inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Backward", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addListener(control, "Forward", "Backward", "Left", "Right");
    }

    private void shoot() {
        Sphere sphere = new Sphere(8, 16, 0.2f);
        Geometry bullet = new Geometry("Bullet", sphere);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", assetManager.loadTexture("Textures/spectralSphere.jpg"));
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

    private ActionListener actionListenerShoot = new ActionListener() {
        public void onAction(String name, boolean isPressed, float tpf) {
            if (name.equals("Shoot") && isPressed) {
                shoot();
            }
        }
    };
    
    private ActionListener actionListenerRestart = new ActionListener() {
	public void onAction(String name, boolean isPressed, float tpf) {
            if (name.equals("RestartGame") && isPressed && (juegoGanado || juegoPerdido)) {
                reiniciarJuego();
            }
        }
    };
    
    private PhysicsSpace getPhysicsSpace() {
        return bulletAppState.getPhysicsSpace();
    }

    public void collision(PhysicsCollisionEvent event) {
    if (juegoGanado) return;  
    Spatial a = event.getNodeA();
    Spatial b = event.getNodeB();
    
    if (a == null || b == null) return;

    Spatial bala = null;
    Spatial enemigo = null;
    Spatial jugador = null;

    // Detección de colisión bala-enemigo
    if ("Bullet".equals(a.getName()) && "Enemy".equals(b.getName())) {
        bala = a;
        enemigo = b;
    } else if ("Enemy".equals(a.getName()) && "Bullet".equals(b.getName())) {
        bala = b;
        enemigo = a;
    }

    // Detección de colisión jugador-enemigo
    if (a == player && "Enemy".equals(b.getName())) {
        jugador = a;
        enemigo = b;
    } else if ("Enemy".equals(a.getName()) && b == player) {
        jugador = b;
        enemigo = a;
    }

    if (bala != null && enemigo != null) {
            EnemigoControl ec = enemigo.getControl(EnemigoControl.class);
            if (ec != null && !ec.isDead()) {
                ec.markDead();
                hitSound.playInstance();
                removerEnemigo(enemigo); // Usar el método sincronizado
                addPuntos(10);
                enemigo.rotate(-FastMath.HALF_PI, 0, 0);
                enemigo.addControl(new RemoverDespuesControl(2f, bulletAppState));
                getPhysicsSpace().remove(bala.getControl(RigidBodyControl.class));
                bala.removeFromParent();
            }
        }

    if (jugador != null && enemigo != null) {
    EnemigoControl ec = enemigo.getControl(EnemigoControl.class);
    if (ec != null && !ec.isDead() && ec.canDamage()) {
        damagePlayer(10);
        ec.resetDamageCooldown();
        // Empujar al jugador
        Vector3f pushDirection = player.getWorldTranslation()
            .subtract(enemigo.getWorldTranslation())
            .normalizeLocal()
            .multLocal(5f);
        player.getControl(RigidBodyControl.class).setLinearVelocity(pushDirection);
    }
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
        if (juegoGanado || juegoPerdido) return;
    
        playerHealth -= amount;
        if (playerHealth < 0) playerHealth = 0;
    
        if (healthText != null) {
            healthText.setText("Vida: " + playerHealth);
        }
    
        if (playerHealth == 0 && !juegoPerdido) {
            mostrarMensajeMuerte();
        }
    }
    
    private void initAmbientSound() {
        ambientSound = new AudioNode(assetManager, "Sounds/ambienceLoop.wav", DataType.Stream);
        ambientSound.setLooping(true);            // Repetir en bucle
        ambientSound.setPositional(false);        // No cambia con la posición del jugador
        ambientSound.setVolume(0.5f);             // Ajusta volumen según prefieras
        rootNode.attachChild(ambientSound);       // Adjuntar al nodo raíz
        ambientSound.play();                      // Iniciar reproducción
    }
    
    private void reiniciarJuego() {
	juegoGanado = false;
	juegoPerdido = false;
        puntos = 0;
        enemigosEnEscena = 0;
        bulletsFired = 0;
        playerHealth = 100;
        torre = null;

        // Eliminar todo del rootNode y guiNode
        rootNode.detachAllChildren();
        guiNode.detachAllChildren();
        enemys.clear();
        inputManager.deleteMapping("Shoot");
        inputManager.deleteMapping("RestartGame");

        // Volver a cargar escena, jugador, torre, enemigos, HUD, sonido, etc.
        simpleInitApp();
    }
}