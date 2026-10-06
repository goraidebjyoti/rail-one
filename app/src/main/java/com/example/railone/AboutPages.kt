package com.example.railone

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AboutBlue = Color(0xFF0166FF)
private val AboutInk = Color(0xFF122661)
internal val RAILWAY_SOCIAL_LINKS = listOf(
    "X" to "https://x.com/RailMinIndia",
    "Facebook" to "https://www.facebook.com/RailMinIndia/",
    "Instagram" to "https://www.instagram.com/railminindia/",
    "YouTube" to "https://www.youtube.com/user/RailMinIndia")
internal fun railwaySocialIntent(url: String): Intent {
    require(RAILWAY_SOCIAL_LINKS.any { it.second == url })
    return Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)
}
internal fun railwayMailIntent(): Intent = Intent(Intent.ACTION_SENDTO,
    Uri.parse("mailto:railone.support@cris.org.in?subject=${Uri.encode("SuperApp for Indian Railways :")}&body=${Uri.encode("Write Your Message Here!")}"))
    .putExtra(Intent.EXTRA_SUBJECT, "SuperApp for Indian Railways :")
    .putExtra(Intent.EXTRA_TEXT, "Write Your Message Here!")
internal fun railwayCallIntent(): Intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:139"))

@Composable
private fun AboutHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(AboutBlue).statusBarsPadding().height(68.dp).padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(38.dp).border(.8.dp, Color.White, CircleShape)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 20.dp))
    }
}
@Composable
internal fun AboutPage(onBack: () -> Unit, onSocial: (String) -> Unit, onMail: () -> Unit,
    onCall: () -> Unit, onTerms: () -> Unit, onPrivacy: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFFEFEDFF))) {
        AboutHeader("About", onBack)
        Column(Modifier.weight(1f).navigationBarsPadding().verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(R.drawable.rail_one_logo), "RailOne", Modifier.width(105.dp).height(28.dp))
                Spacer(Modifier.height(18.dp))
                Text("RailOne is a SuperApp by Indian Railways that consolidates all public-facing services into one platform, enhancing user experience with single sign-on access. Users can plan and book train tickets, track trains, order food, and more.",
                    fontSize = 14.sp, lineHeight = 18.sp, color = Color(0xFF62616C), textAlign = TextAlign.Justify)
                Spacer(Modifier.height(48.dp))
            }
            Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).border(.8.dp, Color(0xFFE4E4E4), RoundedCornerShape(20.dp))
                .padding(horizontal = 24.dp, vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Reach Us", color = AboutInk, fontSize = 15.sp)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth().height(58.dp).border(.8.dp, Color(0xFFE4E4E4), RoundedCornerShape(12.dp)), verticalAlignment = Alignment.CenterVertically) {
                    AboutAction("Call 139", Icons.Default.Phone, Modifier.weight(1f), onCall)
                    Box(Modifier.width(1.dp).height(38.dp).background(Color(0xFFE8E8E8)))
                    AboutAction("Write Email", Icons.Default.MailOutline, Modifier.weight(1f), onMail)
                }
                Spacer(Modifier.height(20.dp))
                Text("Stay Connected", color = AboutInk, fontSize = 15.sp)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    RAILWAY_SOCIAL_LINKS.forEachIndexed { index, (name, url) -> SocialLogo(index, name) { onSocial(url) } }
                }
                Spacer(Modifier.height(26.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AboutAction("Terms Of Use", Icons.Default.Description, Modifier.weight(1f), onTerms)
                    AboutAction("Privacy Policy", Icons.Default.PrivacyTip, Modifier.weight(1f), onPrivacy)
                }
                Spacer(Modifier.height(26.dp))
            }
            Column(Modifier.fillMaxWidth().padding(vertical = 42.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CRIS", color = Color(0xFF17669F), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 30.sp)
                Text("making IT happen", color = Color(0xFF17669F), fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 10.sp)
                Spacer(Modifier.height(9.dp))
                Text("Official RailOne developed & hosted by\nCentre for Railway Information Systems", color = Color(0xFF62616C), fontSize = 11.sp, lineHeight = 15.sp, textAlign = TextAlign.Center)
            }
        }
    }
}
@Composable
private fun AboutAction(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Row(modifier.heightIn(min = 42.dp).clickable(onClick = onClick).padding(horizontal = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Icon(icon, null, tint = Color(0xFF9990EC), modifier = Modifier.size(20.dp))
        Text(label, color = Color.Black, fontSize = 13.sp, lineHeight = 16.sp, modifier = Modifier.padding(start = 7.dp))
    }
}
@Composable
private fun SocialLogo(index: Int, name: String, onClick: () -> Unit) {
    val backgrounds = listOf(Color(0xFFE3E3E3), Color(0xFFE4EBF5), Color(0xFFFFEFF1), Color(0xFFFFE2E4))
    Box(Modifier.size(54.dp).background(backgrounds[index], RoundedCornerShape(15.dp)).clickable(onClick = onClick).semantics { contentDescription = "Ministry of Railways on $name" }, contentAlignment = Alignment.Center) {
        when(index) {
            0 -> Box(Modifier.size(34.dp).background(Color.Black, CircleShape), contentAlignment = Alignment.Center) { Text("𝕏", color = Color.White, fontSize = 22.sp) }
            1 -> Box(Modifier.size(34.dp).background(Color(0xFF3B51A1), CircleShape), contentAlignment = Alignment.Center) { Text("f", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 36.sp) }
            else -> Canvas(Modifier.size(34.dp)) {
                if (index == 2) {
                    drawRoundRect(Brush.linearGradient(listOf(Color(0xFF635AD1), Color(0xFFD239AE), Color(0xFFFCB94F))), cornerRadius = CornerRadius(size.width * .24f))
                    val margin = size.width * .16f
                    drawRoundRect(Color.White, Offset(margin, margin), Size(size.width - margin * 2, size.height - margin * 2), CornerRadius(size.width * .19f), style = Stroke(size.width * .06f))
                    drawCircle(Color.White, size.width * .17f, style = Stroke(size.width * .06f))
                    drawCircle(Color.White, size.width * .045f, Offset(size.width * .73f, size.height * .28f))
                } else {
                    drawRoundRect(Color(0xFFFF243B), Offset(0f,size.height*.08f), Size(size.width,size.height*.84f), CornerRadius(size.width*.21f))
                    drawPath(Path().apply { moveTo(size.width*.4f,size.height*.3f); lineTo(size.width*.4f,size.height*.7f); lineTo(size.width*.72f,size.height*.5f); close() }, Color.White)
                }
            }
        }
        // The parent semantics identify each external platform without showing extra labels.
    }
}

@Composable
internal fun RailwayLegalPage(privacy: Boolean, onBack: () -> Unit, onOfficialPolicy: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        AboutHeader(if (privacy) "Privacy Policy" else "Terms Of Use", onBack)
        Column(Modifier.weight(1f).navigationBarsPadding().verticalScroll(rememberScrollState()).padding(15.dp)) {
            Card(shape = RoundedCornerShape(9.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                Column {
                    Text("Rail One local app", color = Color(0xFF777777), fontSize = 9.sp, modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp))
                    TextButton(onClick = onOfficialPolicy, modifier = Modifier.padding(horizontal = 9.dp)) { Text("Official RailOne Privacy Policy", fontSize = 11.sp) }
                    if (privacy) {
                        LegalParagraph(PRIVACY_INTRO)
                        PRIVACY_POINTS.forEach { point -> Row(Modifier.padding(horizontal = 27.dp, vertical = 4.dp)) {
                            Text("•", fontSize = 10.sp, color = Color(0xFF62616C), modifier = Modifier.padding(end = 14.dp))
                            Text(point, fontSize = 10.sp, lineHeight = 12.sp, color = Color(0xFF62616C))
                        } }
                        Spacer(Modifier.height(20.dp))
                    } else {
                        LegalParagraph(TERMS_INTRO)
                        TERMS_SECTIONS.forEach { (heading, body) ->
                            Text(heading, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AboutInk,
                                modifier = Modifier.fillMaxWidth().background(Color(0xFFE1F8FE)).padding(horizontal = 15.dp, vertical = 16.dp))
                            LegalParagraph(body)
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun LegalParagraph(text: String) {
    Text(text, fontSize = 10.sp, lineHeight = 12.sp, color = Color(0xFF62616C), modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 28.dp))
}
