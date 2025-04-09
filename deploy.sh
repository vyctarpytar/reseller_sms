git add .
git commit -m "Updated :- Reseller was not updating new value for allocatable units"
git fetch
git merge
eval "$(ssh-agent -s)"
ssh-add /home/vyctarpytar/.ssh/vyctarpytar_github

git push