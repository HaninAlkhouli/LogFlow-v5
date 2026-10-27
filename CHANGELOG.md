# Değişiklik Günlüğü

## v5

- Recoverable kayıt hataları için `StageException`, fatal sistem hataları için `PipelineException` ayrımı yapıldı.
- YAML aşamalarına `FAIL_FAST`, `SKIP`, `DEAD_LETTER` ve `RETRY(n)` politikaları eklendi.
- Ham girdi, aşama adı, hata ve zamanı taşıyan bağımsız dead-letter kanalı eklendi.
- Zorunlu alan, aralık, timestamp ve IPv4 kurallarını YAML'dan alan `ValidationStage` eklendi.
- Girdi, çıktı, aşama bazlı drop, dead-letter ve gerçek süreyi raporlayan `RunSummary` eklendi.
- Geçersiz UTF-8'i kaybetmeden okuyabilmek için kaynak ham byte satırları üretir hale getirildi.
- Kirli fixture ve boş dosya için chaos testleri eklendi; `11 = 3 + 0 + 8` uzlaştırması doğrulandı.
- Maven proje sürümü `5.0.0` olarak güncellendi.

## v4

- Pipeline topolojisi YAML yapılandırma dosyalarına taşındı.
- Veri odaklı `StageFactory` ve `StageRegistry` eklendi.
- Şema, parametre ve aşama tiplerini çalıştırmadan önce doğrulayan `PipelineLoader` eklendi.
- Hata raporu, API trafiği ve istemci bant genişliği için üç yapılandırma eklendi.
- Bilinmeyen aşama, eksik parametre, yanlış parametre türü ve boş pipeline hataları test edildi.
- Aynı çalıştırılabilir JAR'ın üç yapılandırmayla farklı çıktı üretmesi doğrulandı.
- Maven proje sürümü `4.0.0` olarak güncellendi ve çalıştırılabilir fat JAR eklendi.
- Terminale, doğrulanmış YAML içeriğinden üretilen gerçek çalışma planı eklendi.
- Silinen aşamanın planda bulunmadığı ve eksik `stages` bölümünün çalıştırmadan önce reddedildiği test edildi.

## v3

- Farklı veri tiplerini sıralı bağlayan fluent `PipelineBuilder` eklendi.
- Predicate tabanlı `PredicateFilterStage` eklendi.
- Alan seçimi ve veri minimizasyonu için `ProjectionStage` ile `ProjectedRecord` eklendi.
- `open()` / `close()` yaşam döngüsünü kullanan UTF-8 `FileSink` eklendi.
- `--min-status`, `--path-prefix` ve `--fields` CLI seçenekleri eklendi.
- Her yeni bileşen için birim testleri ve üç aşamalı entegrasyon testi eklendi.
- Filtre sırası deneyi ve sonucu `ARCHITECTURE.md` içinde belgelendi.
- Maven proje sürümü `3.0.0` olarak güncellendi.

## v2

Increment 2 kapsamında değiştirilen ve eklenen dosyalar:

- `src/LogRecord.java` - Immutable alan kaydı.
- `src/ParserStage.java` - CLF/Combined Log Format ayrıştırıcısı ve hatalı kayıt sayacı.
- `src/ConsoleSink.java` - `LogRecord` değerlerini tek satırda yazdırma.
- `src/Pipeline.java` - Tipli aşama listesi ve kaynak-mesaj akışı.
- `src/Main.java` - ParserStage eklenmesi ve hatalı kayıt sayısının gösterilmesi.
- `src/Record.java` - Domain kayıtları için ortak marker arayüzü.
- `src/test/java/ParserStageTest.java` - JUnit 5 parser testleri.
- `pom.xml` - JUnit 5, Maven ve JaCoCo yapılandırması.
- `data/access-small.log` - Combined Log Format örnek verileri.
- `README.md` - Çalıştırma, test ve coverage açıklamaları.
- `ARCHITECTURE.md` - Increment 2 mimari açıklaması.
- `.gitignore` - Maven `target` klasörü.
