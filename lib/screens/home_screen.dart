import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  Future<void> _signOut() async {
    await FirebaseAuth.instance.signOut();
  }

  @override
  Widget build(BuildContext context) {
    final user = FirebaseAuth.instance.currentUser;
    final categories = <_IssueCategory>[
      const _IssueCategory(
        title: 'Dziura w drodze',
        icon: Icons.construction_rounded,
        color: Color(0xFFD97A73),
      ),
      const _IssueCategory(
        title: 'Smieci',
        icon: Icons.delete_sweep_rounded,
        color: Color(0xFF74B67C),
      ),
      const _IssueCategory(
        title: 'Zloz wniosek',
        icon: Icons.description_rounded,
        color: Color(0xFF6FA3D9),
      ),
      const _IssueCategory(
        title: 'Niedzialajace oswietlenie',
        icon: Icons.lightbulb_circle_rounded,
        color: Color(0xFFDFA06A),
      ),
      const _IssueCategory(
        title: 'Uszkodzony chodnik',
        icon: Icons.directions_walk_rounded,
        color: Color(0xFFB088D1),
      ),
      const _IssueCategory(
        title: 'Wandalizm',
        icon: Icons.gpp_bad_rounded,
        color: Color(0xFFA98D81),
      ),
      const _IssueCategory(
        title: 'Niedrozna kanalizacja',
        icon: Icons.water_drop_rounded,
        color: Color(0xFF5CAFA4),
      ),
    ];

    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Text(user?.email ?? 'UrbanFix'),
        actions: [
          if (user != null)
            Padding(
              padding: const EdgeInsets.only(right: 4.0),
              child: IconButton(
                icon: const Icon(Icons.logout),
                tooltip: 'Sign out',
                onPressed: _signOut,
              ),
            ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: GridView.builder(
          itemCount: categories.length,
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 2,
            crossAxisSpacing: 14,
            mainAxisSpacing: 14,
            childAspectRatio: 1.35,
          ),
          itemBuilder: (context, index) {
            final category = categories[index];
            return FilledButton(
              style: FilledButton.styleFrom(
                backgroundColor: category.color,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.all(12),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(18),
                ),
              ),
              onPressed: () {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('Wybrano: ${category.title}')),
                );
              },
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(category.icon, size: 36),
                  const SizedBox(height: 10),
                  Text(
                    category.title,
                    textAlign: TextAlign.center,
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      color: Colors.white,
                      fontWeight: FontWeight.w700,
                      fontSize: 16,
                    ),
                  ),
                ],
              ),
            );
          },
        ),
      ),
    );
  }
}

class _IssueCategory {
  final String title;
  final IconData icon;
  final Color color;

  const _IssueCategory({
    required this.title,
    required this.icon,
    required this.color,
  });
}
