<?php
$host     = 'localhost'; 
$dbname   = 'almica_db';
$username = 'almica_db';
$password = 'Edc4#rfv';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username, $password, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION
    ]);

    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        // Parameter auslesen und in eine Ganzzahl (Integer) umwandeln. Standard: 3 Tage
        $days = isset($_POST['days']) ? filter_var($_POST['days'], FILTER_VALIDATE_INT) : 3;

        if ($days === false || $days < 0) {
            http_response_code(400);
            echo json_encode(["status" => "error", "message" => "Ungültige Anzahl an Tagen übermittelt."]);
            exit;
        }

        // SQL-Befehl mit Platzhalter (:days) für die Tage
        $sql = "DELETE FROM locations WHERE created_at < NOW() - INTERVAL :days DAY";
        $stmt = $pdo->prepare($sql);
        
        // Parameter sicher binden
        $stmt->bindValue(':days', $days, PDO::PARAM_INT);
        $stmt->execute();
        
        $deletedRows = $stmt->rowCount();

        echo json_encode([
            "status" => "success", 
            "message" => "$deletedRows veraltete Standorte (älter als $days Tage) erfolgreich gelöscht."
        ]);
    } else {
        http_response_code(405);
        echo json_encode(["status" => "error", "message" => "Nur POST-Anfragen erlaubt."]);
    }
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(["status" => "error", "message" => "Datenbankfehler: " . $e->getMessage()]);
}
?>
