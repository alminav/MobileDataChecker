<?php
header('Content-Type: application/json; charset=utf-8');

$host     = 'localhost'; 
$dbname   = 'almica_db';
$username = 'almica_db';
$password = 'Edc4#rfv';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username, $password, [
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    ]);

    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        $id = isset($_POST['id']) ? filter_var($_POST['id'], FILTER_VALIDATE_INT) : false;

        if ($id === false) {
            http_response_code(400);
            echo json_encode(["status" => "error", "message" => "Ungültige ID angegeben."]);
            exit;
        }

        // Datensatz löschen
        $stmt = $pdo->prepare("DELETE FROM locations WHERE id = :id");
        $stmt->execute([':id' => $id]);

        echo json_encode(["status" => "success", "message" => "Eintrag erfolgreich gelöscht."]);
    } else {
        http_response_code(405);
        echo json_encode(["status" => "error", "message" => "Nur POST erlaubt."]);
    }

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(["status" => "error", "message" => "Datenbankfehler: " . $e->getMessage()]);
}
?>
