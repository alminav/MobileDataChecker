<!DOCTYPE html>
<html lang="de">
<head>
    <meta charset="UTF-8">
    <title>Bildergalerie mit Download und Lösch-Button</title>
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
        .image-card img {
            width: 100%;
            height: 320px;
            object-fit: cover;
        }
        .image-info {
            padding: 10px;
            font-size: 13px;
            color: #555555;
            background-color: #fafafa;
            border-top: 1px solid #eeeeee;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .date-text {
            flex-grow: 1;
            text-align: left;
            font-size: 12px;
        }
        .action-buttons {
            display: flex;
            gap: 8px;
        }
        .action-btn {
            text-decoration: none;
            font-size: 16px;
            padding: 4px 6px;
            border-radius: 4px;
            transition: background-color 0.2s;
            border: none;
            background: transparent;
            cursor: pointer;
        }
        /* Styling für den Download-Button */
        .download-btn:hover {
            background-color: #e2e6ea;
        }
        /* Styling für den Lösch-Button */
        .delete-btn:hover {
            background-color: #f8d7da;
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

// 1. LÖSCH-LOGIK
if (isset($_GET['delete'])) {
    $fileToDelete = $_GET['delete'];
    if (strpos($fileToDelete, $dir) === 0 && file_exists($fileToDelete)) {
        if (unlink($fileToDelete)) {
            $msg = "<div class='alert alert-success'>Das Bild wurde erfolgreich gelöscht!</div>";
        } else {
            $msg = "<div class='alert alert-danger'>Fehler: Das Bild konnte nicht gelöscht werden.</div>";
        }
    }
}

// 2. Sortierung auslesen
$sortOrder = isset($_GET['sort']) && $_GET['sort'] === 'oldest' ? 'oldest' : 'newest';

// 3. Bilder einlesen
$pattern = $dir . "*.{jpg,jpeg,png,gif,webp,JPG,JPEG,PNG,GIF,WEBP}";
$images = glob($pattern, GLOB_BRACE);

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
            $formattedDate = date("d.m.Y H:i", $fileTime);
            $filename = basename($image);

            $deleteUrl = "?sort=" . $sortOrder . "&delete=" . urlencode($image);
			// Die URL für das Sende-Skript mit dem Parameter "datei" aufbauen
			$sendEmailUrl = "send_email.php?datei=" . urlencode($image);
            echo '<div class="image-card">';
            echo '  <img src="' . htmlspecialchars($image) . '" alt="Galeriebild">';
            echo '  <div class="image-info">';
            echo '      <span class="date-text">' . $formattedDate . ' Uhr</span>';
            echo '      <div class="action-buttons">';
			// 2. NEUER E-Mail-Button (leitet an send_email.php weiter)
			echo '          <a href="' . htmlspecialchars($sendEmailUrl) . '" class="action-btn email-btn" title="Als E-Mail-Anhang senden">✉️</a>';

            // Download-Button
            echo '          <a href="' . htmlspecialchars($image) . '" download="' . htmlspecialchars($filename) . '" class="action-btn download-btn" title="Bild herunterladen">💾</a>';
            // Neuer Lösch-Button (ruft weiterhin die Sicherheitsabfrage auf)
            echo '          <button class="action-btn delete-btn" title="Bild löschen" onclick="confirmDelete(\'' . jsone(htmlspecialchars($deleteUrl)) . '\')">🗑️</button>';
            echo '      </div>';
            echo '  </div>';
            echo '</div>';
        }
    } else {
        echo '<p style="text-align:center; grid-column: 1/-1;">Keine Bilder im Verzeichnis gefunden.</p>';
    }

    function jsone($str) {
        return str_replace("'", "\'", $str);
    }
    ?>
</div>

<script>
function confirmDelete(deleteUrl) {
    if (confirm("Möchtest du dieses Bild wirklich unwiderruflich löschen?")) {
        window.location.href = deleteUrl;
    }
}
</script>

</body>
</html>
