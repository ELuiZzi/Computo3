package Utils;

public class AlgoritmoPrecios {

    // Definición de márgenes base por categoría (Estrategia conservadora vs agresiva)
    // Formato: {Margen Minimo %, Margen Ideal %}
    private static final double[] MARGEN_CABLES = {1.00, 2.50};      // 100% a 250% (Alta rentabilidad)
    private static final double[] MARGEN_ACCESORIOS = {0.50, 1.00};  // 50% a 100%
    private static final double[] MARGEN_PERIFERICOS = {0.30, 0.50}; // 30% a 50%
    private static final double[] MARGEN_REFACCIONES = {0.35, 0.60}; // 35% a 60%
    private static final double[] MARGEN_HARDWARE = {0.15, 0.25};    // 15% a 25% (Alta competencia)
    private static final double[] MARGEN_EQUIPOS = {0.10, 0.20};     // 10% a 20% (Volumen alto, margen bajo)

    public static class ResultadoPrecio {
        public double precioSugerido;
        public double margenAplicado; // En porcentaje (ej: 45.5)
        public String explicacion; // Para mostrar al usuario por qué se eligió ese precio
    }

    public static ResultadoPrecio calcular(String categoria, double costo, double mercadoMin, double mercadoProm, double mercadoMax) {
        ResultadoPrecio res = new ResultadoPrecio();
        double[] rangoMargen;

        // 1. Seleccionar Estrategia según Categoría
        switch (categoria) {
            case "Cables y adaptadores": rangoMargen = MARGEN_CABLES; break;
            case "Accesorios pequeños": rangoMargen = MARGEN_ACCESORIOS; break;
            case "Periféricos": rangoMargen = MARGEN_PERIFERICOS; break;
            case "Refacciones": rangoMargen = MARGEN_REFACCIONES; break;
            case "Hardware": rangoMargen = MARGEN_HARDWARE; break;
            case "Equipos grandes": rangoMargen = MARGEN_EQUIPOS; break;
            default: rangoMargen = new double[]{0.30, 0.40}; break; // Default 30-40%
        }

        // 2. Calcular Precio Ideal (Basado solo en mi costo y mi deseo de ganar)
        double precioIdeal = costo * (1 + rangoMargen[1]); // Intentamos ganar el máximo del rango

        // 3. Comparar con el Mercado (La realidad)

        // Caso A: No hay datos de mercado (Investigación vacía), usamos nuestro ideal.
        if (mercadoProm <= 0) {
            res.precioSugerido = precioIdeal;
            res.margenAplicado = rangoMargen[1] * 100;
            res.explicacion = "Basado puramente en costo (Sin ref. mercado).";
            return res;
        }

        // Caso B: Mi precio ideal es MUY barato comparado con el mercado (Dejamos dinero en la mesa)
        // Estrategia: Subir un poco para acercarse al promedio, pero seguir siendo barato.
        if (precioIdeal < mercadoMin) {
            res.precioSugerido = (precioIdeal + mercadoMin) / 2;
            res.explicacion = "Ajustado hacia arriba: Tu costo permite un precio muy competitivo.";
        }
        // Caso C: Mi precio ideal es MUY caro (No venderé nada)
        // Estrategia: Bajar al margen mínimo aceptable o igualar el promedio si es posible.
        else if (precioIdeal > mercadoProm) {
            double precioMinimoViable = costo * (1 + rangoMargen[0]);

            if (precioMinimoViable > mercadoProm) {
                // ALERTA: Aún con margen mínimo, somos caros. Igualamos al promedio pero avisamos.
                res.precioSugerido = mercadoProm;
                res.explicacion = "¡Cuidado! Tu costo es alto. Se igualó al promedio de mercado.";
            } else {
                // Podemos bajar un poco para ser competitivos
                res.precioSugerido = mercadoProm * 0.95; // 5% abajo del promedio
                res.explicacion = "Precio competitivo (5% bajo el promedio de mercado).";
            }
        }
        // Caso D: Estamos en el rango dulce
        else {
            res.precioSugerido = precioIdeal;
            res.explicacion = "Precio óptimo según estrategia de categoría.";
        }

        // Calcular margen final real
        res.margenAplicado = ((res.precioSugerido - costo) / costo) * 100;

        // Redondeo estético (ej: 199.99 en lugar de 198.43)
        res.precioSugerido = redondearPrecio(res.precioSugerido);

        return res;
    }

    private static double redondearPrecio(double precio) {
        // Lógica simple de redondeo comercial (terminar en .00, .50 o .90)
        double entero = Math.floor(precio);
        double decimal = precio - entero;

        if (decimal < 0.25) return entero;
        if (decimal < 0.75) return entero + 0.50;
        return entero + 1.00; // Redondear al siguiente entero
    }
}