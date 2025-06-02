package mygame;

import com.jme3.scene.control.AbstractControl;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;

public class TorreControl extends AbstractControl {

    private int vida = 100;
    private boolean destruida = false;

    public void recibirDanno(int cantidad) {
        if (!destruida) {
            vida -= cantidad;
            System.out.println("Torre recibió daño. Vida restante: " + vida);
            if (vida <= 0) {
                vida = 0;
                destruirTorre();
            }
        }
    }

    private void destruirTorre() {
        destruida = true;
        System.out.println("¡La torre ha sido destruida!");
        // Aquí puedes poner animaciones o acciones de final del juego
        spatial.removeFromParent(); // opcional: eliminar torre del juego
    }

    public int getVida() {
        return vida;
    }

    @Override
    protected void controlUpdate(float tpf) {
        // Lógica por frame (opcional)
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}
}
