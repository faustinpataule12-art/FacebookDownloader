package com.npsai.fbdownloader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Facebook Downloader
 * Créer par NPS.NELSON
 *
 * Flux : coller un lien -> rechercher -> choisir un format vidéo (MP4)
 * ou audio (MP3) parmi ceux réellement disponibles pour cette vidéo,
 * avec la taille exacte -> télécharger. Le fichier final est toujours
 * préfixé "NPS_" (signature).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var editLink: EditText
    private lateinit var buttonSearch: ImageButton
    private lateinit var buttonSettings: ImageButton
    private lateinit var textHint: TextView
    private lateinit var progressSearch: ProgressBar
    private lateinit var layoutResultats: LinearLayout
    private lateinit var textVideoTitle: TextView
    private lateinit var headerVideoSection: LinearLayout
    private lateinit var textVideoBadge: TextView
    private lateinit var listVideoFormats: LinearLayout
    private lateinit var headerAudioSection: LinearLayout
    private lateinit var textAudioBadge: TextView
    private lateinit var listAudioFormats: LinearLayout
    private lateinit var buttonDownload: Button
    private lateinit var progressDownload: ProgressBar
    private lateinit var textStatus: TextView
    private lateinit var buttonWhatsapp: ImageButton
    private lateinit var viewPulse: View

    private var videoInfo: VideoInfo? = null
    private var selectedFormat: VideoFormat? = null
    private var selectedIsAudio = false

    companion object {
        private const val WHATSAPP_ABOUT_LINK =
            "https://whatsapp.com/channel/0029VbCuah30gcfNKZjZtP05"
        private const val WHATSAPP_CONTACT_NUMBER = "243901352535"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        startPulseAnimation()

        buttonSearch.setOnClickListener { rechercherVideo() }
        buttonSettings.setOnClickListener { ouvrirParametres() }
        buttonWhatsapp.setOnClickListener { ouvrirWhatsappContact() }
        buttonDownload.setOnClickListener { telechargerFormatSelectionne() }
        headerVideoSection.setOnClickListener {
            toggleSection(listVideoFormats)
        }
        headerAudioSection.setOnClickListener {
            toggleSection(listAudioFormats)
        }
    }

    private fun bindViews() {
        editLink = findViewById(R.id.editLink)
        buttonSearch = findViewById(R.id.buttonSearch)
        buttonSettings = findViewById(R.id.buttonSettings)
        textHint = findViewById(R.id.textHint)
        progressSearch = findViewById(R.id.progressSearch)
        layoutResultats = findViewById(R.id.layoutResultats)
        textVideoTitle = findViewById(R.id.textVideoTitle)
        headerVideoSection = findViewById(R.id.headerVideoSection)
        textVideoBadge = findViewById(R.id.textVideoBadge)
        listVideoFormats = findViewById(R.id.listVideoFormats)
        headerAudioSection = findViewById(R.id.headerAudioSection)
        textAudioBadge = findViewById(R.id.textAudioBadge)
        listAudioFormats = findViewById(R.id.listAudioFormats)
        buttonDownload = findViewById(R.id.buttonDownload)
        progressDownload = findViewById(R.id.progressDownload)
        textStatus = findViewById(R.id.textStatus)
        buttonWhatsapp = findViewById(R.id.buttonWhatsapp)
        viewPulse = findViewById(R.id.viewPulse)
    }

    // ---------- Halo animé du bouton WhatsApp ----------
    private fun startPulseAnimation() {
        val scale = ScaleAnimation(
            1f, 1.8f, 1f, 1.8f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        val fade = AlphaAnimation(0.6f, 0f)
        val set = AnimationSet(true).apply {
            addAnimation(scale)
            addAnimation(fade)
            duration = 1400
            repeatCount = Animation.INFINITE
        }
        viewPulse.startAnimation(set)
    }

    private fun ouvrirWhatsappContact() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://wa.me/$WHATSAPP_CONTACT_NUMBER")
        )
        startActivity(intent)
    }

    // ---------- Paramètres ----------
    private fun ouvrirParametres() {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this)
            .inflate(R.layout.bottom_sheet_settings, null)
        dialog.setContentView(view)

        view.findViewById<ImageButton>(R.id.buttonCloseSettings)
            .setOnClickListener { dialog.dismiss() }

        view.findViewById<LinearLayout>(R.id.buttonAbout)
            .setOnClickListener {
                startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(WHATSAPP_ABOUT_LINK))
                )
                dialog.dismiss()
            }

        dialog.show()
    }

    // ---------- Recherche de la vidéo ----------
    private fun rechercherVideo() {
        val url = editLink.text.toString().trim()
        if (url.isEmpty()) {
            Toast.makeText(this, "Colle un lien d'abord", Toast.LENGTH_SHORT).show()
            return
        }

        textHint.visibility = View.GONE
        layoutResultats.visibility = View.GONE
        progressSearch.visibility = View.VISIBLE
        selectedFormat = null

        lifecycleScope.launch {
            try {
                val info = withContext(Dispatchers.IO) {
                    YoutubeDL.getInstance().getInfo(url)
                }
                videoInfo = info
                afficherResultats(info)
            } catch (e: Exception) {
                progressSearch.visibility = View.GONE
                textHint.visibility = View.VISIBLE
                textHint.text = "Impossible de trouver cette vidéo.\n" +
                        "Vérifie que le lien est public et correct.\n(${e.message})"
            }
        }
    }

    private fun afficherResultats(info: VideoInfo) {
        progressSearch.visibility = View.GONE
        layoutResultats.visibility = View.VISIBLE
        textVideoTitle.text = info.title ?: "Vidéo trouvée"

        val formats = info.formats ?: emptyList()

        // Formats vidéo : ceux qui ont un flux vidéo (vcodec renseigné et != "none")
        val videoFormats = formats
            .filter { it.vcodec != null && it.vcodec != "none" }
            .distinctBy { it.height }
            .sortedBy { it.height ?: 0 }

        // Formats audio seul : pas de vidéo mais un flux audio
        val audioFormats = formats
            .filter { (it.vcodec == null || it.vcodec == "none") && it.acodec != null && it.acodec != "none" }
            .distinctBy { it.abr }
            .sortedBy { it.abr ?: 0.0 }

        remplirListe(
            listVideoFormats, videoFormats, isAudio = false,
            libelle = { f -> "${f.height ?: "?"}p${if ((f.height ?: 0) >= 720) " HD" else ""}" }
        )
        remplirListe(
            listAudioFormats, audioFormats, isAudio = true,
            libelle = { f -> "${f.abr?.toInt() ?: "?"} kbps" }
        )

        if (videoFormats.isNotEmpty()) {
            selectedFormat = videoFormats.last()
            selectedIsAudio = false
            mettreAJourBadges()
        }
    }

    private fun remplirListe(
        conteneur: LinearLayout,
        liste: List<VideoFormat>,
        isAudio: Boolean,
        libelle: (VideoFormat) -> String
    ) {
        conteneur.removeAllViews()
        if (liste.isEmpty()) {
            val vide = TextView(this).apply {
                text = "Aucun format disponible"
                setTextColor(getColor(R.color.neutral_400))
                textSize = 12f
                setPadding(12, 12, 12, 12)
            }
            conteneur.addView(vide)
            return
        }

        for (format in liste) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(24, 20, 24, 20)
                background = getDrawable(
                    if (isAudio) R.drawable.bg_row_selected_orange
                    else R.drawable.bg_row_selected_blue
                )
                background.alpha = 0
                isClickable = true
                isFocusable = true
            }

            val label = TextView(this).apply {
                text = libelle(format)
                setTextColor(getColor(R.color.neutral_700))
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
            }

            val size = TextView(this).apply {
                text = formaterTaille(format.filesize ?: format.filesizeApprox)
                setTextColor(getColor(R.color.neutral_400))
                textSize = 12f
            }

            row.addView(label)
            row.addView(size)

            row.setOnClickListener {
                selectedFormat = format
                selectedIsAudio = isAudio
                mettreAJourBadges()
                surlignerSelection(conteneur, row)
            }

            conteneur.addView(row)
        }
    }

    private fun surlignerSelection(conteneur: LinearLayout, selectedRow: View) {
        for (i in 0 until conteneur.childCount) {
            val child = conteneur.getChildAt(i)
            child.background?.alpha = if (child == selectedRow) 255 else 0
        }
    }

    private fun mettreAJourBadges() {
        val format = selectedFormat ?: return
        if (selectedIsAudio) {
            textAudioBadge.text = "${format.abr?.toInt() ?: "?"} kbps"
            textVideoBadge.text = ""
        } else {
            textVideoBadge.text = "${format.height ?: "?"}p"
            textAudioBadge.text = ""
        }
    }

    private fun toggleSection(section: LinearLayout) {
        section.visibility = if (section.visibility == View.VISIBLE) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    private fun formaterTaille(bytes: Long?): String {
        if (bytes == null || bytes <= 0) return "Taille inconnue"
        val mb = bytes / (1024.0 * 1024.0)
        return String.format(Locale.FRANCE, "%.1f MB", mb)
    }

    // ---------- Téléchargement ----------
    private fun telechargerFormatSelectionne() {
        val info = videoInfo ?: return
        val format = selectedFormat
        if (format == null) {
            Toast.makeText(this, "Choisis un format d'abord", Toast.LENGTH_SHORT).show()
            return
        }

        val url = editLink.text.toString().trim()
        val titreBrut = (info.title ?: "video").replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val extension = if (selectedIsAudio) "mp3" else "mp4"
        val nomFichier = "NPS_$titreBrut.$extension"

        val dossier = if (selectedIsAudio) {
            getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        } else {
            getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        } ?: filesDir

        progressDownload.visibility = View.VISIBLE
        textStatus.text = "Téléchargement en cours..."
        buttonDownload.isEnabled = false

        lifecycleScope.launch {
            try {
                val request = YoutubeDLRequest(url).apply {
                    addOption("-o", File(dossier, nomFichier).absolutePath)
                    addOption("--restrict-filenames")
                    if (selectedIsAudio) {
                        addOption("-f", format.formatId ?: "bestaudio")
                        addOption("-x")
                        addOption("--audio-format", "mp3")
                    } else {
                        addOption("-f", "${format.formatId}+bestaudio/best")
                        addOption("--merge-output-format", "mp4")
                    }
                }

                withContext(Dispatchers.IO) {
                    YoutubeDL.getInstance().execute(request) { _, _, line ->
                        runOnUiThread { textStatus.text = line }
                    }
                }

                textStatus.text = "Terminé ! Enregistré : ${nomFichier}"
                Toast.makeText(this@MainActivity, "Téléchargement réussi", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                textStatus.text = "Erreur : ${e.message}"
            } finally {
                progressDownload.visibility = View.GONE
                buttonDownload.isEnabled = true
            }
        }
    }
}
