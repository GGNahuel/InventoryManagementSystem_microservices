package com.nahuelgg.inventory_app.products.components;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import com.nahuelgg.inventory_app.products.entities.ProductEntity;
import com.nahuelgg.inventory_app.products.repositories.ProductRepository;

import lombok.RequiredArgsConstructor;

@Component
@ConditionalOnExpression("!'${spring.profiles.active:}'.contains('test')")
@RequiredArgsConstructor
public class DatabaseFillerWithExampleData implements CommandLineRunner{
  private final ProductRepository productRepository;
  
  @Override
  public void run(String... args) throws Exception {
    final UUID accountId = UUID.fromString("12341234-0000-0000-0000-11223344acc1");

    if (!productRepository.findByAccountId(accountId).isEmpty()) return;

    ProductEntity ibuprofeno = ProductEntity.builder()
      .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
      .name("Ibuprofeno 400mg")
      .brand("Genérico")
      .model("Caja 20 comprimidos")
      .description("Analgesico y antiinflamatorio")
      .unitPrice(250.0)
      .categories(List.of("Medicamentos", "Analgesicos"))
      .accountId(accountId)
    .build();

    ProductEntity paracetamol = ProductEntity.builder()
      .id(UUID.fromString("00000000-0000-0000-0000-000000000002"))
      .name("Paracetamol 500mg")
      .brand("Genérico")
      .model("Caja 10 comprimidos")
      .description("Analgésico y antipirético")
      .unitPrice(180.0)
      .categories(List.of("Medicamentos", "Analgesicos"))
      .accountId(accountId)
    .build();

    ProductEntity alcoholGel = ProductEntity.builder()
      .id(UUID.fromString("00000000-0000-0000-0000-000000000003"))
      .name("Alcohol en gel")
      .brand("SanitPlus")
      .model("Botella 250ml")
      .description("Gel antibacterial al 70%")
      .unitPrice(500.0)
      .categories(List.of("Higiene", "Desinfectantes"))
      .accountId(accountId)
    .build();

    ProductEntity termometro = ProductEntity.builder()
      .id(UUID.fromString("00000000-0000-0000-0000-000000000004"))
      .name("Termómetro digital")
      .brand("Omron")
      .model("MC-246")
      .description("Medición rápida en 60 segundos")
      .unitPrice(2000.0)
      .categories(List.of("Equipos Médicos"))
      .accountId(accountId)
    .build();

    ProductEntity guantesLatex = ProductEntity.builder()
      .id(UUID.fromString("00000000-0000-0000-0000-000000000005"))
      .name("Guantes de látex")
      .brand("SafeHands")
      .model("Talle M - Caja 100u")
      .description("Uso médico descartable")
      .unitPrice(3500.0)
      .categories(List.of("Higiene", "Descartables"))
      .accountId(accountId)
    .build();

    ProductEntity mascarillaQuirurgica = ProductEntity.builder()
      .id(UUID.fromString("00000000-0000-0000-0000-000000000006"))
      .name("Mascarilla quirúrgica")
      .brand("MediMask")
      .model("Caja 50u")
      .description("Mascarilla descartable triple capa")
      .unitPrice(1200.0)
      .categories(List.of("Protección Personal"))
      .accountId(accountId)
    .build();

    ProductEntity aspirina = ProductEntity.builder()
      .id(UUID.fromString("00000000-0000-0000-0000-000000000007"))
      .name("Aspirina 100mg")
      .brand("Bayer")
      .model("Caja 30 comprimidos")
      .description("Analgésico y anticoagulante")
      .unitPrice(600.0)
      .categories(List.of("Medicamentos", "Cardiología"))
      .accountId(accountId)
    .build();

    ProductEntity jarabeTos = ProductEntity.builder()
      .id(UUID.fromString("00000000-0000-0000-0000-000000000008"))
      .name("Jarabe para la tos")
      .brand("Bisolvon")
      .model("Frasco 120ml")
      .description("Expectorante")
      .unitPrice(1500.0)
      .categories(List.of("Medicamentos", "Respiratorio"))
      .accountId(accountId)
    .build();

    ProductEntity vitaminaC = ProductEntity.builder()
      .id(UUID.fromString("00000000-0000-0000-0000-000000000009"))
      .name("Vitamina C 1g")
      .brand("Redoxon")
      .model("Tubo 10 comprimidos efervescentes")
      .description("Suplemento vitamínico")
      .unitPrice(950.0)
      .categories(List.of("Suplementos", "Vitaminas"))
      .accountId(accountId)
    .build();

    ProductEntity shampoo = ProductEntity.builder()
      .id(UUID.fromString("00000000-0000-0000-0000-000000000010"))
      .name("Shampoo Anticaspa")
      .brand("Head & Shoulders")
      .model("Botella 400ml")
      .description("Cuidado del cuero cabelludo")
      .unitPrice(1800.0)
      .categories(List.of("Higiene", "Cuidado Personal"))
      .accountId(accountId)
    .build();

    ProductEntity enjuagueBucal = ProductEntity.builder()
      .id(UUID.fromString("00000000-0000-0000-0000-000000000011"))
      .name("Enjuague bucal")
      .brand("Listerine")
      .model("Botella 500ml")
      .description("Protección antibacterial 24hs")
      .unitPrice(1200.0)
      .categories(List.of("Higiene", "Bucal"))
      .accountId(accountId)
    .build();

    ProductEntity cremaHidratante = ProductEntity.builder()
      .id(UUID.fromString("00000000-0000-0000-0000-000000000012"))
      .name("Crema hidratante corporal")
      .brand("Nivea")
      .model("Pote 200ml")
      .description("Cuidado diario de la piel")
      .unitPrice(950.0)
      .categories(List.of("Cosmética", "Cuidado Personal"))
      .accountId(accountId)
    .build();

    ProductEntity protectorSolar = ProductEntity.builder()
      .id(UUID.fromString("00000000-0000-0000-0000-000000000013"))
      .name("Protector solar FPS 50")
      .brand("La Roche-Posay")
      .model("Frasco 200ml")
      .description("Alta protección UVA/UVB")
      .unitPrice(5500.0)
      .categories(List.of("Cosmética", "Protección Solar"))
      .accountId(accountId)
    .build();

    ProductEntity pañales = ProductEntity.builder()
      .id(UUID.fromString("00000000-0000-0000-0000-000000000014"))
      .name("Pañales talla M")
      .brand("Pampers")
      .model("Paquete 30u")
      .description("Ultra absorbentes")
      .unitPrice(4500.0)
      .categories(List.of("Bebés"))
      .accountId(accountId)
    .build();

    ProductEntity lechePolvo = ProductEntity.builder()
      .id(UUID.fromString("00000000-0000-0000-0000-000000000015"))
      .name("Leche en polvo infantil")
      .brand("Nestlé")
      .model("Lata 800g")
      .description("Fórmula de crecimiento")
      .unitPrice(7500.0)
      .categories(List.of("Bebés", "Alimentos"))
      .accountId(accountId)
    .build();

    ProductEntity glucometro = ProductEntity.builder()
      .id(UUID.fromString("00000000-0000-0000-0000-000000000016"))
      .name("Glucómetro")
      .brand("Accu-Chek")
      .model("Guide Me")
      .description("Medición rápida de glucosa en sangre")
      .unitPrice(9000.0)
      .categories(List.of("Equipos Médicos", "Diabetes"))
      .accountId(accountId)
    .build();

    ProductEntity testCovid = ProductEntity.builder()
      .id(UUID.fromString("00000000-0000-0000-0000-000000000017"))
      .name("Test rápido COVID-19")
      .brand("Roche")
      .model("Kit individual")
      .description("Autotest antígeno nasal")
      .unitPrice(3200.0)
      .categories(List.of("Test Rápidos", "Descartables"))
      .accountId(accountId)
    .build();

    ProductEntity vendas = ProductEntity.builder()
      .id(UUID.fromString("00000000-0000-0000-0000-000000000018"))
      .name("Venda elástica")
      .brand("Curaflex")
      .model("10cm x 5m")
      .description("Uso médico y deportivo")
      .unitPrice(700.0)
      .categories(List.of("Descartables", "Ortopedia"))
      .accountId(accountId)
    .build();

    ProductEntity suero = ProductEntity.builder()
      .id(UUID.fromString("00000000-0000-0000-0000-000000000019"))
      .name("Suero fisiológico")
      .brand("B. Braun")
      .model("Botella 500ml")
      .description("Solución salina estéril")
      .unitPrice(900.0)
      .categories(List.of("Medicamentos", "Soluciones"))
      .accountId(accountId)
    .build();

    ProductEntity colirio = ProductEntity.builder()
      .id(UUID.fromString("00000000-0000-0000-0000-000000000020"))
      .name("Colirio lágrimas artificiales")
      .brand("Systane")
      .model("Frasco 10ml")
      .description("Lubricante ocular")
      .unitPrice(2200.0)
      .categories(List.of("Oftalmología"))
      .accountId(accountId)
    .build();

    productRepository.saveAll(List.of(
      ibuprofeno, paracetamol, alcoholGel, termometro, guantesLatex, mascarillaQuirurgica, aspirina, jarabeTos, vitaminaC, shampoo,
      enjuagueBucal, protectorSolar, cremaHidratante, pañales, lechePolvo, glucometro, testCovid, vendas, suero, colirio
    ));
  }
}
