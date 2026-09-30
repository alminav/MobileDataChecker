<?php
// 1. Datenbank-Zugangsdaten von bplaced einfügen
$host     = 'localhost'; 
$dbname   = 'almica_db';
$username = 'almica_db';
$password = 'Edc4#rfv';

// Verbindung zur Datenbank herstellen
try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username, $password, [
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    ]);
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(["status" => "error", "message" => "Datenbankverbindung fehlgeschlagen."]);
    exit;
}

// 2. POST-Parameter empfangen und validieren
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    
    // Den zu löschenden Titel auslesen
    $title = isset($_POST['title']) ? trim($_POST['title']) : '';

    // Prüfen, ob überhaupt ein Titel übergeben wurde
    if (empty($title)) {
        http_response_code(400);
        echo json_encode(["status" => "error", "message" => "Kein Titel zum Löschen angegeben."]);
        exit;
    }

    // 3. Datensätze mit LIKE (Teiltext-Suche) löschen
    try {
        // SQL-Befehl mit LIKE vorbereiten
        $sql = "DELETE FROM locations WHERE title LIKE :title";
        $stmt = $pdo->prepare($sql);
        
        // Die Wildcards (%) um den Suchbegriff herum konkatenieren
        $searchTerm = "%" . $title . "%";
        
        $stmt->execute([
            ':title' => $searchTerm
        ]);

        // Prüfen, wie viele Datensätze gelöscht wurden
        $deletedRows = $stmt->rowCount();

        if ($deletedRows > 0) {
            echo json_encode([
                "status" => "success", 
                "message" => "$deletedRows Standort(e), die '$title' enthalten, wurden erfolgreich gelöscht."
            ]);
        } else {
            echo json_encode([
                "status" => "success", 
                "message" => "Keine Einträge mit diesem Teiltext im Titel gefunden."
            ]);
        }
        
    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode(["status" => "error", "message" => "Fehler beim Löschen: " . $e->getMessage()]);
    }

} else {
    http_response_code(405);
    echo json_encode(["status" => "error", "message" => "Nur POST-Anfragen sind erlaubt."]);
}
?>
