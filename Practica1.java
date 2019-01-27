import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/*PAU PÉREZ*/

/*PRÀTICA 1 - MUNTANYA RUSA*/

 /*Es disposa d’un vagó en el que caben 4 passatgers que ha de donar servei a un nombre
indeterminat d’usuaris, el vagó només circula quan va ple. La simulació s’inicia amb l’arribada
d’un nombre aleatori de passatgers, en arribar cada passatger saluda i s’identifica. Després de
saludar, els passatgers esperen a que arribi el vagó. El vagó avisa quan es posa en marxa,
depenent del nombre de passatgers el vagó calcula el nombre de viatges que haurà de fer i, si el
nombre de viatgers no és múltiple de 4, els passatgers que no podran viatjar. Després que el
vagó es posi en mode de càrrega els passatgers començaran a pujar, quan el quart passatger
hagi pujat, el vagó partirà al seu destí. Pacientment els passatgers esperaran que el vagó arribi i
es posi en mode descàrrega, en aquest moment baixaran i s’acomiadaran. Mentre quedin grups
de 4 passatgers el vagó anirà fent successius torns i quan hagi acabat s’aturarà. Si queden
passatgers sense viatjar també es s’acomiadaran.*/

public class Practica1 implements Runnable {

    private static final int MAXPASSATGERS = 20;
    private static final int VAGONS = 1;
    public static final int PASSATGERS = generatePassatgers();
    private static final int FILS = VAGONS + PASSATGERS;
    private static final int MAXPERVAGO = 4;

    static Semaphore potsBaixar = new Semaphore(1);
    static Semaphore potsPujar = new Semaphore(0);
    static Semaphore vagoMoviment = new Semaphore(0);

    private static String nomPassatgers[] = new String[MAXPASSATGERS];

    private static int repetits[] = new int[PASSATGERS];
    static int index = 0;

    static final int totalviatges = PASSATGERS / MAXPERVAGO;

    static volatile int viatges = 0;
    static volatile int passatgers = 0;

    int id;
    String nom;

    private Practica1(int id, String nom) {
        this.id = id;
        this.nom = nom;
    }

    public static void main(String[] args) throws InterruptedException {
        loadNames(nomPassatgers);
        Thread[] threads = new Thread[FILS];

        for (int i = 0; i < VAGONS; i++) {
            threads[i] = new Thread(new Practica1(i, null));
            threads[i].start();
            System.out.println("El vagó esta llest per començar");
        }

        for (int i = 0 + VAGONS; i < FILS; i++) {
            String persona = generateName();
            threads[i] = new Thread(new Practica1(i, persona));
            threads[i].start();
            System.out.println(persona + " està passejant pel parc d'atraccions");
        }
        
        System.out.println();

        for (int j = 0; j < FILS; j++) {
            threads[j].join();
        }
        
        System.out.println();

        System.out.println("PLANEJAT:");
        System.out.println("Total viatges: " + totalviatges);
        System.out.println("Passatgers: " + PASSATGERS);

        System.out.println("REALITZAT:");
        System.out.println("Total viatges: " + viatges);
        System.out.println("Passatgers: " + (MAXPERVAGO * viatges));
        System.out.println("Passatgers que no han pujat: " + PASSATGERS % MAXPERVAGO);
    }

    @Override
    public synchronized void run() {
        if (id == 0) {
            try {
                Vago();
            } catch (InterruptedException ex) {
                Logger.getLogger(Practica1.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            try {
                Passatger();
            } catch (InterruptedException ex) {
                Logger.getLogger(Practica1.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void Vago() throws InterruptedException {
        if (PASSATGERS >= 4) {
            while (viatges != totalviatges) {
                Thread.sleep(1000);
                carrega();
                marxa();
                descarrega();
            }
        } else {
            Thread.sleep(300);
            System.out.println("Atracció: No hi ha gent suficient!");
        }
        Thread.sleep(1000); // dona el permís perque els passatgers que no han 
        potsPujar.release(); // pogut pujar acabin
    }

    private void Passatger() throws InterruptedException {
        if (totalviatges != 0) {
            Thread.sleep(1000);
            potsPujar.acquire(); // Els passatgers esperen el permis del vagó
            if (viatges != totalviatges) {
                passatgers++;
                if (passatgers == MAXPERVAGO) {
                    System.out.println(nom + " entra dins el vagó");
                    System.out.println(nom + " era el darrer per entrar");
                    System.out.println();
                    vagoMoviment.release(); // dona el permis al vago per començar
                } else {
                    if (viatges != totalviatges) {
                        System.out.println(nom + " entra dins el vagó");
                        potsPujar.release();
                    }
                }
                potsBaixar.acquire(); // Espera a que el vago doni permis per baixar
                passatgers--;
                System.out.println("El passatger " + nom + " ha baixat");
                System.out.println(nom + ": A reveure!");
                if (passatgers == 0) {
                    System.out.println();
                }
                potsBaixar.release();// dona permis al següent per baixar
            } else {
                potsPujar.release();
                System.out.println("El passatger " + nom + " no ha pogut pujar");
            }
        } else {
            System.out.println(nom + ": No podem pujar");
        }
    }

    private void carrega() throws InterruptedException {
        potsBaixar.acquire(); // mira si tots els passatgers han baixat
        System.out.println("Vagó preparat...");
        passatgers = 0;
        potsPujar.release();
    }

    private void marxa() throws InterruptedException {
        vagoMoviment.acquire(); // EL vagó espera a que el darrer passatger entri
        for (int i = 0; i < 3; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Practica1.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("El vagó està en funcionament");
        }
        System.out.println();
        viatges++;
    }

    private void descarrega() throws InterruptedException {
        potsBaixar.release(); // dona el permís per baixar del vagó
    }

    private static void loadNames(String[] names) {
        File file = new File("noms.txt");
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String linia;
            String total = "";
            String nom = "";
            int comptador = 0;
            while ((linia = br.readLine()) != null) {
                total += linia;
            }
            for (int i = 0; total.charAt(i) != '.'; i++) {
                if (total.charAt(i) != ',' && total.charAt(i) != ' ') {
                    nom += total.charAt(i);
                } else if (total.charAt(i) != ' ') {
                    names[comptador] = nom;
                    comptador++;
                    nom = "";
                }
            }
            names[comptador] = nom;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Practica1.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Practica1.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static String generateName() {
        int p = (int) (Math.random() * MAXPASSATGERS);
        for (int i = 0; i < index; i++) {
            if (repetits[i] == p) {
                p = (int) (Math.random() * MAXPASSATGERS);     // Aquest mètode genera noms aleatoris no repetits
                i = -1;
            }
        }
        repetits[index] = p;
        index++;
        return nomPassatgers[p];
    }

    private static int generatePassatgers() {
        return (int) (Math.random() * MAXPASSATGERS);
    }

}
