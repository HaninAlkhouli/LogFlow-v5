# LogFlow v5

LogFlow v5, web sunucusu loglarını YAML ile tanımlanan sıralı bir pipeline içinde
işler. Bu sürümün ana amacı, tek bir bozuk kaydın bütün çalışmayı durdurmasını
önlemek ve her girdinin sonucunu sayılabilir hale getirmektir.

Gerçek çalışma sırası:

`FileLineSource -> ParserStage -> ValidationStage -> ProjectionStage -> FileSink`

Hatalı kayıtlar ana akıştan ayrılarak bağımsız `DeadLetterFileSink` dosyasına
yazılır.

## Gereksinimler

- Java JDK 21 veya daha yeni sürüm
- İnternet bağlantısı (Maven Wrapper ilk kullanımda Maven ve bağımlılıkları indirir)

## Derleme ve test

Windows PowerShell üzerinde proje klasöründe:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd package
```

Oluşan çalıştırılabilir dosya:

`target/logflow-5.0.0.jar`

Linux/macOS için `mvnw.cmd` yerine `./mvnw` kullanılır.

## Chaos testini çalıştırma

```powershell
java -jar target\logflow-5.0.0.jar --config configs\chaos-test.yaml
```

Çıktıları görmek için:

```powershell
Get-Content out\valid-records.txt
Get-Content out\dead-letter.txt
```

Beklenen özet:

```text
input records: 11
output records: 3
dead-letter records: 8
counts reconcile: true
```

Üç geçerli kayıt `out/valid-records.txt` dosyasına yazılır. Sekiz hatalı kayıt
`out/dead-letter.txt` dosyasında hata aşaması, hata açıklaması, zaman ve ham
girdi ile birlikte tutulur.

## Boş dosya testi

```powershell
java -jar target\logflow-5.0.0.jar --config configs\empty-input.yaml
```

Bu çalışma hata vermeden tamamlanır ve girdi/çıktı/dead-letter sayaçlarının
tamamı sıfır olur.

## YAML hata politikaları

Her aşama kendi politikasını `onError` alanından alır:

- `FAIL_FAST`: Fatal hata üretir ve pipeline durur.
- `SKIP`: Hatalı kayıt atlanır ve o aşamanın drop sayacı artar.
- `DEAD_LETTER`: Hatalı kayıt bağımsız dead-letter kanalına gönderilir.
- `RETRY(n)`: Kayıt en fazla `n` kez yeniden denenir; yine başarısızsa fatal hata olur.

`ValidationStage` kuralları Java koduna gömülü değildir. Zorunlu alanlar, byte
ve status aralıkları, timestamp sınırları ve IPv4 denetimi YAML içinden okunur.

## Önemli dosyalar

- `configs/chaos-test.yaml`: v5 için ana test yapılandırması.
- `data/access-dirty.log`: Bozuk ve geçerli kayıtların birlikte bulunduğu fixture.
- `src/StageNode.java`: Aşama politikalarını kayıt bazında uygular.
- `src/ValidationStage.java`: Bildirimsel doğrulama kurallarını uygular.
- `src/RunSummary.java`: Girdi, çıktı, drop, dead-letter ve süre sayılarını tutar.
- `ARCHITECTURE.md`: Mimari kararlar ve hata-politika tablosu.

Bu proje Increment 5 teslimidir ve Git etiketi `v5` olarak hazırlanmalıdır.
