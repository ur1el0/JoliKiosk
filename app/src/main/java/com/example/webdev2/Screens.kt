package com.example.webdev2

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

data class Branch(
    val name: String,
    val address: String,
    @DrawableRes val imageRes: Int,
)

private val branches = listOf(
    Branch(
        name = "SM City Lucena",
        address = "Maharlika Highway, Lucena City",
        imageRes = R.drawable.sm_lucena,
    ),
    Branch(
        name = "Lucena Bayan",
        address = "Quezon Avenue, Lucena City",
        imageRes = R.drawable.lucena_bayan,
    ),
)

private fun peso(centavos: Int): String = NumberFormat.getCurrencyInstance(Locale("en", "PH"))
    .format(centavos / 100.0)

@Composable
fun BranchSelectionScreen(onBranchSelected: (Branch) -> Unit) {
    Column(Modifier.fillMaxSize().background(BgGray)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Pink, PinkLight)))
                .padding(horizontal = 20.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Welcome!", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Select your pickup branch", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
        }

        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            items(branches, key = { it.name }) { branch ->
                BranchCard(branch = branch, onClick = { onBranchSelected(branch) })
            }
        }
    }
}

@Composable
private fun BranchCard(branch: Branch, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (pressed) 0.97f else 1f)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
    ) {
        Image(
            painter = painterResource(branch.imageRes),
            contentDescription = branch.name,
            modifier = Modifier.fillMaxWidth().height(160.dp),
            contentScale = ContentScale.Crop,
        )
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Pink, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(branch.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
            }
            Text(branch.address, fontSize = 13.sp, color = TextGray, modifier = Modifier.padding(start = 28.dp))
        }
    }
}

@Composable
fun MenuScreen(
    menu: List<MenuItem>,
    quantities: Map<Int, Int>,
    branch: Branch,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onChangeBranch: () -> Unit,
    onAdd: (MenuItem) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filteredMenu = menu.filter { item ->
        query.isBlank() || item.name.contains(query, ignoreCase = true) ||
            item.category.contains(query, ignoreCase = true)
    }

    Column(Modifier.fillMaxSize().background(BgGray)) {
        MenuHeader(branch, onChangeBranch)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Search menu") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(14.dp),
        )
        when {
            isLoading -> Loading()
            error != null -> ErrorState(error, onRetry)
            filteredMenu.isEmpty() -> EmptyState("No menu items found")
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filteredMenu, key = { it.id }) { item ->
                    MenuCard(item, quantities[item.id] ?: 0) { onAdd(item) }
                }
            }
        }
    }
}

@Composable
private fun MenuHeader(branch: Branch, onChangeBranch: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(Pink, PinkLight)))
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Ordering from ${branch.name}", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            Text("Kiosk Menu", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onChangeBranch,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        ) {
            Text("Change", fontSize = 11.sp)
        }
    }
}

@Composable
private fun MenuCard(item: MenuItem, quantity: Int, onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(menuImage(item)),
                contentDescription = item.name,
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold, color = TextDark)
                Text(
                    item.description,
                    fontSize = 12.sp,
                    color = TextGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(5.dp))
                Text(peso(item.priceInCentavos), fontWeight = FontWeight.ExtraBold, color = Pink)
            }
            Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Pink)) {
                Text(if (quantity > 0) "Add ($quantity)" else "Add")
            }
        }
    }
}

@Composable
fun CartScreen(
    menu: List<MenuItem>,
    quantities: Map<Int, Int>,
    isSubmitting: Boolean,
    error: String?,
    onQuantityChange: (Int, Int) -> Unit,
    onCheckout: () -> Unit,
) {
    val lines = menu.mapNotNull { item -> quantities[item.id]?.takeIf { it > 0 }?.let { item to it } }
    val total = lines.sumOf { (item, quantity) -> item.priceInCentavos * quantity }
    Column(Modifier.fillMaxSize().background(BgGray)) {
        Header("Your Cart", "Review your order")
        if (lines.isEmpty()) {
            EmptyState("Your cart is empty")
            return@Column
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(lines, key = { it.first.id }) { (item, quantity) ->
                CartRow(item, quantity, onQuantityChange)
            }
        }
        error?.let {
            Text(it, color = Color(0xFFB3261E), modifier = Modifier.padding(horizontal = 20.dp))
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(peso(total), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Pink)
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onCheckout,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Pink),
                ) {
                    Text(if (isSubmitting) "Submitting…" else "Place Order")
                }
            }
        }
    }
}

@Composable
private fun CartRow(item: MenuItem, quantity: Int, onQuantityChange: (Int, Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(menuImage(item)),
                contentDescription = item.name,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold)
                Text(peso(item.priceInCentavos), color = TextGray, fontSize = 13.sp)
            }
            IconButton(onClick = { onQuantityChange(item.id, quantity - 1) }) {
                Icon(Icons.Default.Remove, contentDescription = "Remove one")
            }
            Text(quantity.toString(), fontWeight = FontWeight.Bold)
            IconButton(onClick = { onQuantityChange(item.id, quantity + 1) }) {
                Icon(Icons.Default.Add, contentDescription = "Add one")
            }
        }
    }
}

@Composable
fun OrderConfirmation(order: SubmittedOrder, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Order received") },
        text = {
            Column {
                Text("Your queue number is", color = TextGray)
                Text("#${order.queueNumber}", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Pink)
                Text("Status: ${order.status.replaceFirstChar { it.uppercase() }}")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Pink)) {
                Text("Done")
            }
        },
    )
}

@Composable
private fun Header(title: String, subtitle: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(Pink, PinkLight)))
            .padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
        Text(subtitle, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Loading() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    CircularProgressIndicator(color = Pink)
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) =
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = TextDark)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }

@Composable
private fun EmptyState(message: String) =
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = TextGray)
    }

@DrawableRes
private fun menuImage(item: MenuItem): Int {
    val name = item.name.lowercase()
    return when {
        "burgersteak" in name || "burger steak" in name -> R.drawable.burgersteak
        "bucket" in name -> R.drawable.chickenbucket
        "burger" in name -> R.drawable.yumburger
        "fries" in name -> R.drawable.smallfries
        "spaghetti" in name -> R.drawable.spagdrink
        "float" in name || "cola" in name || "coke" in name -> R.drawable.cokefloat
        "chicken" in name -> R.drawable.chimkenrice
        else -> when (item.category.lowercase()) {
            "burger" -> R.drawable.yumburger
            "sides" -> R.drawable.smallfries
            "drinks" -> R.drawable.cokefloat
            else -> R.drawable.chimkenrice
        }
    }
}
