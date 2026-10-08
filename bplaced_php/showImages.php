<!DOCTYPE html>
<html lang="de">
<head>
    <meta charset="UTF-8">
    <title>Bildergalerie mit Löschfunktion</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 0;
            padding: 20px;
            background-color: #f4f4f9;
        }
        .sort-controls {
            margin-bottom: 20px;
            text-align: center;
        }
        .sort-btn {
            background-color: #ffffff;
            color: #333333;
            border: 2px solid #007bff;
            padding: 8px 16px;
            margin: 0 5px;
            border-radius: 4px;
            cursor: pointer;
            font-size: 14px;
            font-weight: bold;
            transition: all 0.2s ease;
            text-decoration: none;
            display: inline-block;
        }
        .sort-btn:hover {
            background-color: #007bff;
            color: white;
        }
        .sort-btn.active {
            background-color: #007bff;
            color: white;
            cursor: default;
        }
        .gallery {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
            gap: 20px;
        }
        .image-card {
            background: #ffffff;
            border-radius: 8px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
            overflow: hidden;
            display: flex;
            flex-direction: column;
            position: relative;
        }
        /* Der Mauszeiger wird zur Hand, um Klickbarkeit zu signalisieren */
        .image-card img {
            width: 100%;
            height: 320px;
            object-fit: cover;
            cursor: pointer;
            transition: opacity 0.2s;
        }
        /* Visuelles Feedback beim Drüberfahren (Hover) */
        .image-card img:hover {
            opacity: 0.85;
        }
        /* Ein kleiner Lösch-Hinweis, der beim Drüberfahren erscheint */
        .image-card::after {
            content: "🗑️ Klicken zum Löschen";
            position: absolute;
            top: 10px;
            right: 10px;
            background: rgba(220, 53, 69, 0.9);
            color: white;
            padding: 4px 8px;
            font-size: 11px;
            border-radius: 4px;
            pointer-events: none;
            opacity: 0;
            transition: opacity 0.2s;
        }
        .image-card:hover::after {
            opacity: 1;
        }
        .image-info {
            padding: 10px;
            font-size: 13px;
            color: #555555;
            text-align: center;
            background-color: #fafafa;
            border-top: 1px solid #eeeeee;
        }
        .alert {
            padding: 12px;
            margin-bottom: 20px;
            border-radius: 4px;
            text-align: center;
            font-weight: bold;
        }
        .alert-success { background-color: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
        .alert-danger { background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
    </style>
</head>
<body>

<?php
$dir = "uploads/"; 
$msg = "";

// 1. LÖSCH-LOGIK (Wird ausgeführt, wenn ?delete=... in der URL übergeben wird)
if (isset($_GET['delete'])) {
    $fileToDelete = $_GET['delete'];

    // Sicherheitsprüfung: Verhindert, dass Dateien außerhalb des uploads-Ordners gelöscht werden
    if (strpos($fileToDelete, $dir) === 0 && file_exists($fileToDelete)) {
        if (unlink($fileToDelete)) {
            $msg = "<div class='alert alert-success'>Das Bild wurde erfolgreich gelöscht!</div>";
        } else {
            $msg = "<div class='alert alert-danger'>Fehler: Das Bild konnte nicht gelöscht werden. Berechtigungen prüfen!</div>";
        }
    }
}

// 2. Sortierung aus der URL auslesen
$sortOrder = isset($_GET['sort']) && $_GET['sort'] === 'oldest' ? 'oldest' : 'newest';

// 3. Bilder einlesen
$pattern = $dir . "*.{jpg,jpeg,png,gif,webp,JPG,JPEG,PNG,GIF,WEBP}";
$images = glob($pattern, GLOB_BRACE);

// Erfolgs- oder Fehlermeldung ausgeben
echo $msg;
?>

<!-- Sortier-Buttons -->
<div class="sort-controls">
    <a href="?sort=newest" class="sort-btn <?php echo $sortOrder === 'newest' ? 'active' : ''; ?>">
        📅 Neueste zuerst
    </a>
    <a href="?sort=oldest" class="sort-btn <?php echo $sortOrder === 'oldest' ? 'active' : ''; ?>">
        ⏳ Älteste zuerst
    </a>
</div>

<div class="gallery">
    <?php
    if ($images) {
        // Sortier-Logik
        usort($images, function($a, $b) use ($sortOrder) {
            $timeA = filemtime($a);
            $timeB = filemtime($b);
            if ($timeA == $timeB) return 0;
            return ($sortOrder === 'newest') ? (($timeA < $timeB) ? 1 : -1) : (($timeA > $timeB) ? 1 : -1);
        });

        // Bilder ausgeben
        foreach ($images as $image) {
            $fileTime = filemtime($image);
            $formattedDate = date("d.m.Y \u\m H:i \U\h\r", $fileTime);

            // Aktuelle Sortierung beibehalten, wenn die Seite nach dem Löschen neu lädt
            $deleteUrl = "?sort=" . $sortOrder . "&delete=" . urlencode($image);

            echo '<div class="image-card">';
            // Per JavaScript-onclick wird vor dem Weiterleiten nachgefragt
            echo '  <img src="' . htmlspecialchars($image) . '" alt="Galeriebild" onclick="confirmDelete(\'' . jsone(htmlspecialchars($deleteUrl)) . '\')">';
            echo '  <div class="image-info">' . $formattedDate . '</div>';
            echo '</div>';
        }
    } else {
        echo '<p style="text-align:center; grid-column: 1/-1;">Keine Bilder im Verzeichnis gefunden.</p>';
    }

    // Hilfsfunktion zur korrekten Maskierung im JavaScript-Aufruf
    function jsone($str) {
        return str_replace("'", "\'", $str);
    }
    ?>
</div>

<script>
// Sicherheitsabfrage im Browser
function confirmDelete(deleteUrl) {
    if (confirm("Möchtest du dieses Bild wirklich unwiderruflich löschen?")) {
        window.location.href = deleteUrl;
    }
}
</script>

</body>
</html>
